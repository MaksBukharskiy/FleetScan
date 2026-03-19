package com.fleetScan.taxiService.service.bot;

import com.fleetScan.taxiService.domain.admin.Fleet;
import com.fleetScan.taxiService.domain.autopark.car.CarPhoto;
import com.fleetScan.taxiService.domain.autopark.car.PhotoType;
import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.repository.admin.FleetRepository;
import com.fleetScan.taxiService.repository.autopark.car.CarPhotoRepository;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import com.fleetScan.taxiService.service.blacklist.BlackListService;
import com.fleetScan.taxiService.service.security.AccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {

    private static final String AWAITING_FLEET_NAME = "AWAITING_FLEET_NAME";
    private static final String AWAITING_USER_NAME = "AWAITING_USER_NAME";
    private static final String AWAITING_PHOTO_TYPE = "AWAITING_PHOTO_TYPE";
    private static final String AWAITING_VEHICLE_PLATE = "AWAITING_VEHICLE_PLATE";
    private static final String AWAITING_VEHICLE_MODEL = "AWAITING_VEHICLE_MODEL";

    private final FleetRepository fleetRepository;
    private final DriverRepository driverRepository;
    private final CarPhotoRepository carPhotoRepository;
    private final AccessService accessService;
    private final BlackListService blackListService;

    private final Map<Long, String> userStates = new ConcurrentHashMap<>();
    private final Map<Long, UserRole> pendingRoleByChatId = new ConcurrentHashMap<>();
    private final Map<Long, List<PhotoSize>> pendingPhotosByChatId = new ConcurrentHashMap<>();
    private final Map<Long, Driver> pendingDriversByChatId = new ConcurrentHashMap<>();

    public String getUserState(Long chatId) {
        return userStates.get(chatId);
    }

    public String handleTextMessage(Long chatId, String text) {
        if (text == null || text.isBlank()) {
            return "❌ Пустое сообщение.";
        }

        String state = userStates.get(chatId);
        if (AWAITING_FLEET_NAME.equals(state)) {
            return createNewFleet(chatId, text);
        }
        if (AWAITING_USER_NAME.equals(state)) {
            return addNewUser(chatId, text);
        }
        if (AWAITING_PHOTO_TYPE.equals(state)) {
            return handlePhotoTypeSelection(chatId, text);
        }
        if (AWAITING_VEHICLE_PLATE.equals(state)) {
            return handleVehiclePlateInput(chatId, text);
        }
        if (AWAITING_VEHICLE_MODEL.equals(state)) {
            return handleVehicleModelInput(chatId, text);
        }

        if (text.startsWith("/start")) {
            String[] parts = text.split(" ", 2);
            return parts.length > 1
                    ? handleInviteLink(chatId, parts[1])
                    : handleStartCommand(chatId);
        }
        if ("/help".equals(text)) {
            return help(chatId);
        }
        if ("/add_driver".equals(text)) {
            return startToAddUser(chatId, UserRole.OPERATOR);
        }
        if ("/add_observer".equals(text)) {
            return startToAddUser(chatId, UserRole.OBSERVER);
        }
        if ("/photo".equals(text)) {
            return startPhotoSubmission(chatId);
        }
        if ("/register_car".equals(text)) {
            return startCarRegistration(chatId);
        }
        if (text.startsWith("/blacklist_add")) {
            return handleBlackListAdd(chatId, text);
        }
        if (text.startsWith("/blacklist_remove")) {
            return handleBlackListRemove(chatId, text);
        }
        if (text.startsWith("/status")) {
            return handleStatus(chatId, text);
        }
        if ("/whoami".equals(text)) {
            return whoAmI(chatId);
        }

        return "Неизвестная команда. Используйте /help";
    }

    public String handleStartCommand(Long chatId) {
        Optional<Fleet> fleet = fleetRepository.findByAdminChatId(chatId);
        if (fleet.isPresent()) {
            return "✅ Вы администратор автопарка '" + fleet.get().getName() + "'.\n" + help(chatId);
        }

        Optional<Driver> user = driverRepository.findByChatId(chatId);
        if (user.isPresent()) {
            return "✅ Вы уже зарегистрированы как " + user.get().getRole() + ".\n" + help(chatId);
        }

        userStates.put(chatId, AWAITING_FLEET_NAME);
        return "👋 Введите название автопарка для регистрации администратора:";
    }

    public String createNewFleet(Long chatId, String fleetName) {
        String name = fleetName == null ? "" : fleetName.trim();
        if (name.isBlank() || name.length() < 3) {
            return "❌ Некорректное название. Минимум 3 символа.";
        }
        if (fleetRepository.existsByName(name)) {
            userStates.remove(chatId);
            return "❌ Такой автопарк уже существует.";
        }

        Fleet fleet = new Fleet();
        fleet.setAdminChatId(chatId);
        fleet.setName(name);
        fleet.setPublicId("FLEET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        fleetRepository.save(fleet);

        Driver admin = new Driver();
        admin.setFleet(fleet);
        admin.setName("Admin-" + chatId);
        admin.setChatId(chatId);
        admin.setInviteCode("ADMIN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        admin.setIsActive(true);
        admin.setRole(UserRole.ADMIN);
        driverRepository.save(admin);

        userStates.remove(chatId);
        return "✅ Автопарк создан. ID: " + fleet.getPublicId() + "\n" + help(chatId);
    }

    public String startToAddUser(Long chatId, UserRole role) {
        accessService.requireUser(chatId, UserRole.ADMIN);
        userStates.put(chatId, AWAITING_USER_NAME);
        pendingRoleByChatId.put(chatId, role);
        return "✏️ Введите имя пользователя для роли " + role + ":";
    }

    public String addNewUser(Long adminChatId, String userName) {
        Driver admin = accessService.requireUser(adminChatId, UserRole.ADMIN);
        String normalizedName = userName == null ? "" : userName.trim();
        if (normalizedName.length() < 2) {
            return "❌ Имя слишком короткое.";
        }

        UserRole role = pendingRoleByChatId.getOrDefault(adminChatId, UserRole.OPERATOR);
        Long fleetId = admin.getFleet().getId();
        if (driverRepository.findByFleetIdAndName(fleetId, normalizedName).isPresent()) {
            return "⚠️ Пользователь с таким именем уже есть в этом автопарке.";
        }

        String inviteCode = "INV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Driver user = new Driver();
        user.setFleet(admin.getFleet());
        user.setName(normalizedName);
        user.setInviteCode(inviteCode);
        user.setIsActive(true);
        user.setRole(role);
        driverRepository.save(user);

        userStates.remove(adminChatId);
        pendingRoleByChatId.remove(adminChatId);
        return "✅ Добавлен " + role + ": " + normalizedName + "\n🔗 Ссылка: t.me/FleetScanBot?start=" + inviteCode;
    }

    public String handleInviteLink(Long chatId, String inviteCode) {
        Optional<Driver> inviteUser = driverRepository.findByInviteCode(inviteCode);
        if (inviteUser.isEmpty()) {
            return "❌ Неверная ссылка приглашения.";
        }
        if (driverRepository.findByChatId(chatId).isPresent()) {
            return "ℹ️ Этот Telegram уже зарегистрирован.";
        }

        Driver user = inviteUser.get();
        if (user.getChatId() != null) {
            return "ℹ️ Пользователь уже активирован.";
        }

        user.setChatId(chatId);
        user.setIsActive(true);
        driverRepository.save(user);

        userStates.put(chatId, AWAITING_VEHICLE_PLATE);
        pendingDriversByChatId.put(chatId, user);
        
        return "🎉 Регистрация завершена. Ваша роль: " + user.getRole() + 
               "\n\n🚗 Теперь зарегистрируйте ваш автомобиль.\nВведите номер автомобиля (например, А123БВ777):";
    }

    public void handlePhoto(Long chatId, Message message) {
        Driver user = accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR);
        List<PhotoSize> photos = message.getPhoto();
        PhotoSize photo = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null);
        if (photo == null) {
            return;
        }

        String state = userStates.get(chatId);
        if (AWAITING_PHOTO_TYPE.equals(state)) {
            pendingPhotosByChatId.put(chatId, photos);
            return;
        }

        CarPhoto carPhoto = new CarPhoto();
        carPhoto.setDriver(user);
        carPhoto.setTelegramFileId(photo.getFileId());
        carPhoto.setPhotoType(PhotoType.OTHER);
        carPhoto.setStatus("PENDING");
        carPhotoRepository.save(carPhoto);
    }

    public String startPhotoSubmission(Long chatId) {
        Driver user = accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR);
        userStates.put(chatId, AWAITING_PHOTO_TYPE);
        return """
                📸 Отправка фото автомобиля
                
                Выберите тип фото:
                1️⃣ - Передняя часть
                2️⃣ - Задняя часть
                3️⃣ - Салон
                4️⃣ - Приборная панель
                
                Или отправьте фото сразу, а затем укажите тип.
                """;
    }

    public String handlePhotoTypeSelection(Long chatId, String text) {
        List<PhotoSize> photos = pendingPhotosByChatId.get(chatId);
        if (photos == null || photos.isEmpty()) {
            return "⚠️ Сначала отправьте фото автомобиля.";
        }

        PhotoType selectedType;
        switch (text.trim()) {
            case "1", "1️⃣" -> selectedType = PhotoType.FRONT;
            case "2", "2️⃣" -> selectedType = PhotoType.REAR;
            case "3", "3️⃣" -> selectedType = PhotoType.INTERIOR;
            case "4", "4️⃣" -> selectedType = PhotoType.DASHBOARD;
            default -> {
                selectedType = PhotoType.fromDescription(text);
                if (selectedType == PhotoType.OTHER) {
                    return "❌ Неверный выбор. Введите число 1-4 или название типа фото.";
                }
            }
        }

        Driver user = accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR);
        PhotoSize photo = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElseThrow();

        CarPhoto carPhoto = new CarPhoto();
        carPhoto.setDriver(user);
        carPhoto.setTelegramFileId(photo.getFileId());
        carPhoto.setPhotoType(selectedType);
        carPhoto.setStatus("PENDING");
        carPhotoRepository.save(carPhoto);

        userStates.remove(chatId);
        pendingPhotosByChatId.remove(chatId);

        return "✅ Фото сохранено как: " + selectedType.getDescription();
    }

    public String startCarRegistration(Long chatId) {
        Driver user = accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR);
        if (user.getVehiclePlate() != null && !user.getVehiclePlate().isBlank()) {
            return "ℹ️ Ваш автомобиль уже зарегистрирован: " + user.getVehiclePlate() + 
                   "\nДля изменения используйте /register_car";
        }
        userStates.put(chatId, AWAITING_VEHICLE_PLATE);
        pendingDriversByChatId.put(chatId, user);
        return "🚗 Введите номер автомобиля (например, А123БВ777):";
    }

    public String handleVehiclePlateInput(Long chatId, String text) {
        Driver user = pendingDriversByChatId.get(chatId);
        if (user == null) {
            user = driverRepository.findByChatId(chatId).orElse(null);
            if (user == null) {
                userStates.remove(chatId);
                return "❌ Пользователь не найден. Начните регистрацию заново.";
            }
        }

        String plate = text.trim().toUpperCase();
        if (plate.length() < 5 || plate.length() > 16) {
            return "❌ Неверный формат номера. Введите номер в формате А123БВ777:";
        }

        user.setVehiclePlate(plate);
        pendingDriversByChatId.put(chatId, user);
        userStates.put(chatId, AWAITING_VEHICLE_MODEL);

        return "✅ Номер принят: " + plate + 
               "\n\n🚗 Введите модель автомобиля (например, Kia K5, Hyundai Solaris):";
    }

    public String handleVehicleModelInput(Long chatId, String text) {
        Driver user = pendingDriversByChatId.get(chatId);
        if (user == null) {
            userStates.remove(chatId);
            return "❌ Сессия истекла. Используйте /register_car для начала регистрации.";
        }

        String model = text.trim();
        if (model.length() < 2) {
            return "❌ Слишком короткое название модели.";
        }

        user.setVehicleModel(model);
        driverRepository.save(user);

        userStates.remove(chatId);
        pendingDriversByChatId.remove(chatId);

        return "✅ Автомобиль зарегистрирован!\n" +
               "🚗 Номер: " + user.getVehiclePlate() + "\n" +
               "🚗 Модель: " + user.getVehicleModel();
    }

    private String handleBlackListAdd(Long chatId, String text) {
        String[] parts = text.split("\\s+", 3);
        if (parts.length < 2) {
            return "Использование: /blacklist_add <номер> [причина]";
        }
        String reason = parts.length == 3 ? parts[2] : "manual_block";
        return blackListService.addPlate(chatId, parts[1], reason);
    }

    private String handleBlackListRemove(Long chatId, String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            return "Использование: /blacklist_remove <номер>";
        }
        return blackListService.removePlate(chatId, parts[1]);
    }

    private String handleStatus(Long chatId, String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            return "Использование: /status <номер>";
        }
        return blackListService.statusByPlate(chatId, parts[1]);
    }

    private String whoAmI(Long chatId) {
        return driverRepository.findByChatId(chatId)
                .map(driver -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("👤 ").append(driver.getName())
                      .append("\nРоль: ").append(driver.getRole())
                      .append("\nАктивен: ").append(driver.getIsActive());
                    
                    if (driver.getVehiclePlate() != null) {
                        sb.append("\n🚗 Номер: ").append(driver.getVehiclePlate());
                    }
                    if (driver.getVehicleModel() != null) {
                        sb.append("\n🚗 Модель: ").append(driver.getVehicleModel());
                    }
                    
                    return sb.toString();
                })
                .orElse("❌ Пользователь не зарегистрирован.");
    }

    private String help(Long chatId) {
        UserRole role = driverRepository.findByChatId(chatId).map(Driver::getRole).orElse(null);
        if (role == null) {
            return "Команды:\n/start";
        }
        if (role == UserRole.ADMIN) {
            return """
                    Команды:
                    /add_driver
                    /add_observer
                    /photo - Отправить фото авто
                    /register_car - Зарегистрировать автомобиль
                    /blacklist_add <номер> [причина]
                    /blacklist_remove <номер>
                    /status <номер>
                    /whoami
                    """;
        }
        if (role == UserRole.OPERATOR) {
            return """
                    Команды:
                    /photo - Отправить фото авто
                    /register_car - Зарегистрировать автомобиль
                    /blacklist_add <номер> [причина]
                    /blacklist_remove <номер>
                    /status <номер>
                    /whoami
                    
                    Или просто отправьте фото автомобиля.
                    """;
        }
        return """
                Команды:
                /status <номер>
                /whoami
                """;
    }
}
