package com.fleetScan.taxiService.service.Bot;

import com.fleetScan.taxiService.entity.Admin.Fleet;
import com.fleetScan.taxiService.entity.Autopark.Car.CarPhoto;
import com.fleetScan.taxiService.entity.Autopark.Driver.Driver;
import com.fleetScan.taxiService.repository.Admin.FleetRepository;
import com.fleetScan.taxiService.repository.Autopark.Car.CarPhotoRepository;
import com.fleetScan.taxiService.repository.Autopark.DriverRepository;
import com.fleetScan.taxiService.service.FleetAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {

    private final FleetRepository fleetRepository;
    private final DriverRepository driverRepository;
    private final CarPhotoRepository carPhotoRepository;

    private final Map <Long, String> userStates = new ConcurrentHashMap<>();

    public String getUserState(Long chatId) {
        return userStates.get(chatId);
    }

    private static final String AWAITING_FLEET_NAME = "AWAITING_FLEET_NAME";
    private static final String AWAITING_DRIVER_NAME = "AWAITING_DRIVER_NAME";

    public String handleMessage(Long chatId, String message) {

        if(message == null || message.isBlank()) return "❌ Пустое сообщение.";

        log.info("Получено от {}: {}", chatId, message);

        String state = userStates.get(chatId);

        if (AWAITING_FLEET_NAME.equals(state)) {
            return createNewFleet(chatId, message);
        }
        if (AWAITING_DRIVER_NAME.equals(state)) {
            return addNewDriver(chatId, message);
        }


        if ("/start".equals(message)){

            return handleStartCommand(chatId);
        }
        if ("/add_driver".equals(message)) {
            return startToAddDriver(chatId);
        }

        return "Неизвестная команда. Используйте /start";
    }

    public String handleStartCommand(Long chatId) {

        Optional<Fleet> existingCheckingFleet = fleetRepository.findByAdminChatId(chatId);

        if (existingCheckingFleet.isPresent()) {
            return String.format("✅ Вы уже админ '%s'. ID: %s",
                    existingCheckingFleet.get().getName(), existingCheckingFleet.get().getPublicId());
        }

        userStates.put(chatId, AWAITING_FLEET_NAME);

        return "\uD83D\uDC4B Введите название автопарка: ";
    }

    public String createNewFleet(Long chatId, String name) {
        name = name.trim();

        if (name == null || name.isEmpty() || name.isBlank()) {
            return "Ошибка в имени,\n попробуйте другое ❌";
        }

        if(name.length()<3){
            return "❌ Слишком короткое имя,\nпопробуйте другое";
        }

        if (fleetRepository.existsByName(name)) {
            userStates.remove(chatId);
            return "❌ Уже есть такой автопарк.";
        }

        Fleet fleet = new Fleet();
        fleet.setAdminChatId(chatId);
        fleet.setName(name);
        fleet.setPublicId("TAXI" + (int)(Math.random() * 900 + 100));

        fleetRepository.save(fleet);
        userStates.remove(chatId);

        log.info("Создан автопарк: {} (ID: {})", name, fleet.getPublicId());

        return String.format("✅ Готово! Ваш ID: %s\nТеперь /add_driver", fleet.getPublicId());
    }

    public String startToAddDriver(Long chatId) {

        if(!fleetRepository.existsByAdminChatId(chatId)) {
            return "❌ Сначала создайте автопарк через /start";
        }

        userStates.put(chatId, AWAITING_DRIVER_NAME);

        return "✏️ Введите ФИО водителя: ";
    }

    public String addNewDriver(Long chatId, String driverName) {
        driverName = driverName.trim();

        if (driverName.length() < 2) return "❌ Введите нормальное имя.";

        var fleetOpt = fleetRepository.findByAdminChatId(chatId);
        if (fleetOpt.isEmpty()) return "❌ Не найден ваш автопарк.";

        if (driverRepository.findByName(driverName).isPresent()) {
            return "⚠\uFE0F Такой водитель уже есть.";
        }

        String inviteCode = "INV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Driver driver = new Driver();
        driver.setName(driverName);
        driver.setFleet(fleetOpt.get());
        driver.setInviteCode(inviteCode);
        driver.setIsActive(true);

        driverRepository.save(driver);
        userStates.remove(chatId);

        return String.format("✅ Добавлен: %s\n🔗 Ссылка: t.me/FleetScanBot?start=%s",
                driverName, inviteCode);
    }

    public String handleInviteLink(Long chatId, String inviteCode) {

        var driverOpt = driverRepository.findByInviteCode(inviteCode);

        if (driverOpt.isEmpty()) {
            return "❌ Неверная ссылка.";
        }

        Optional<Driver> existingDriver = driverRepository.findByChatId(chatId);

        if (existingDriver.isPresent()) {
            return "ℹ️ Этот Telegram уже зарегистрирован.";
        }

        Driver driver = driverOpt.get();

        if (driver.getChatId() != null) {
            return "ℹ️ Вы уже зарегистрированы.";
        }

        driver.setChatId(chatId);
        driver.setIsActive(true);
        driverRepository.save(driver);

        return String.format("🎉 Привет, %s! Отправьте фото машины.", driver.getName());
    }


    public void handlePhoto(Long chatId, Message message) {
        List<PhotoSize> photos = message.getPhoto();
        PhotoSize photo = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null);

        if (photo == null) return;

        String fileId = photo.getFileId();
        Optional<Driver> driverOpt = driverRepository.findByChatId(chatId);
        if (driverOpt.isEmpty()) return;

        Driver driver = driverOpt.get();

        CarPhoto carPhoto = new CarPhoto();
        carPhoto.setDriver(driver);
        carPhoto.setTelegramFileId(fileId);
        carPhoto.setStatus("PENDING");
        carPhotoRepository.save(carPhoto);

        log.info("Фото принято от {}: file_id={}", driver.getName(), fileId);
    }

    public String recognizeLicensePlate(java.io.File photoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "tesseract",
                    photoFile.getAbsolutePath(),
                    "stdout",
                    "-l", "rus"
            );

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String result = output.toString().trim();
                log.info("✅ OCR УСПЕШЕН: '{}'", result);
                return result.isEmpty() ? "Номер не найден" : result;
            } else {
                log.error("❌ OCR завершился с ошибкой: {}", exitCode);
                return "Ошибка выполнения";
            }

        } catch (Exception e) {
            log.error("💥 Ошибка при вызове tesseract", e);
            return "Не распознан";
        }
    }

    public String extractLicensePlate(String text) {
        log.info("🔍 Входной текст для поиска номера: '{}'", text);

        text = text.replaceAll("[^АВЕКМНОРСТУХавекмнорстух\\d\\s]", "").toUpperCase();
        log.info("🧹 Очищенный текст: '{}'", text);

        String letters = "АВЕКМНОРСТУХ";
        Pattern pattern = Pattern.compile("[" + letters + "]\\d{3}[" + letters + "]{2}\\d{2,3}");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String result = matcher.group(0);
            log.info("✅ Найден номер: '{}'", result);
            return result;
        }

        log.info("❌ Номер не найден в тексте");
        return "😭 номер не найден,\n попробуйте заново";
    }

}
