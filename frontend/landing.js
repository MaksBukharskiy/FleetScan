// Diesel landing: i18n (ru/uz/en), theme toggle, chat demo, calculator, contacts.

// ---- contacts: fill these in; empty values are hidden ----
const CONTACTS = {
  telegramBot: import.meta.env.VITE_TELEGRAM_BOT_NAME || "", // bot username without @
  telegram: "", // team/support username without @
  email: "",
  phone: "",
};

// Placeholder price for the "Fleet" plan, so'm per car per month.
const PRICE_PER_CAR = 25000;

const T = {
  ru: {
    "nav.how": "Как работает", "nav.features": "Возможности", "nav.pricing": "Тарифы", "nav.contacts": "Контакты", "nav.login": "Войти",
    "hero.pill": "Пилот в Ташкенте · ранний доступ",
    "hero.t1": "Diesel — это", "hero.t2": "ИИ-осмотр автопарка", "hero.t3": "прямо в Telegram",
    "hero.lede": "Водитель присылает фото машины боту — Diesel сам читает номер, ищет повреждения и сверяет с чёрным списком. Менеджер видит только то, что требует внимания.",
    "hero.b1t": "Без приложений", "hero.b1": "водители работают в Telegram, который у них уже есть. Подключение — за один вечер.",
    "hero.b2t": "Узбекские номера", "hero.b2": "01 A 123 BC и 01 123 ABC, все 14 регионов. Номер не совпал — машина уходит на проверку.",
    "hero.b3t": "Повреждения и история", "hero.b3": "вмятины, царапины, стекло, фары, колёса. Каждое фото с датой — меньше споров с водителями.",
    "hero.note": "Пилот бесплатный. Не нужно ни оборудования, ни приложений.",
    "cta.primary": "Подключить автопарк", "cta.login": "Войти в кабинет",
    "demo.status": "бот · онлайн", "demo.foot": "проверка фото", "demo.replay": "повторить",
    "demo.m1": "📷 Фото перед сменой", "demo.m2": "Номер распознан", "demo.m3": "совпадает с регистрацией",
    "demo.m4": "Повреждения: царапина на заднем бампере", "demo.m4s": "незначительная",
    "demo.m5": "Статус: ACTIVE — можно выезжать ✅", "demo.m6": "Хорошей смены!",
    "stats.s1": "типов повреждений", "stats.s2": "регионов Узбекистана", "stats.s3": "приложений для установки", "stats.s4": "бот принимает фото",
    "why.eyebrow": "Зачем это автопарку", "why.title": "осмотр без хаоса в чатах",
    "why.sub": "Сегодня водители шлют фото в общий чат, а менеджер листает сотни сообщений и сверяет номера на глаз. Diesel берёт рутину на себя:",
    "why.c1t": "осмотр за минуту", "why.c1": "Фото → номер, повреждения и статус автоматически. Менеджер смотрит только спорные случаи.",
    "why.c2t": "история каждой машины", "why.c2": "Все фото с датой и водителем. Видно, когда появилась вмятина и кто был за рулём.",
    "why.c3t": "чёрный список", "why.c3": "Заблокированный номер сразу помечается, админ получает уведомление в Telegram.",
    "how.eyebrow": "Как это работает", "how.title": "четыре шага, и автопарк под контролем",
    "how.s1t": "Создайте автопарк", "how.s1": "Напишите боту /start и введите название — вы администратор.",
    "how.s2t": "Пригласите водителей", "how.s2": "Бот даст ссылку-приглашение. Водитель регистрирует номер и модель машины.",
    "how.s3t": "Водители шлют фото", "how.s3": "Перед сменой или по напоминанию. ИИ проверяет номер, повреждения и чёрный список.",
    "how.s4t": "Вы видите главное", "how.s4": "Уведомления о проблемах в Telegram, история и отчёты в Excel — в веб-кабинете.",
    "cmp.eyebrow": "Сравнение", "cmp.title": "чат с фото против Diesel", "cmp.old": "Фото в общем чате", "cmp.new": "Diesel",
    "cmp.r1a": "Номер сверяют на глаз", "cmp.r1b": "Номер читает ИИ и сверяет с регистрацией",
    "cmp.r2a": "Повреждения замечают, когда уже поздно", "cmp.r2b": "Каждое фото проверяется на 6 типов повреждений",
    "cmp.r3a": "Фото теряются в переписке", "cmp.r3b": "История по каждой машине и водителю",
    "cmp.r4a": "Чёрный список в голове у менеджера", "cmp.r4b": "Блокировка и мгновенное уведомление",
    "cmp.r5a": "Отчёты собирают вручную", "cmp.r5b": "Выгрузка в Excel в один клик",
    "who.eyebrow": "Для кого", "who.title": "для всех, у кого много машин",
    "who.a1t": "Таксопарки", "who.a1": "Ежедневный осмотр перед сменой и контроль подмены машин.",
    "who.a2t": "Доставка", "who.a2": "Курьерские машины и фургоны: состояние и ответственность водителя.",
    "who.a3t": "Лизинг и прокат", "who.a3": "Фото при выдаче и возврате — понятно, кто платит за ремонт.",
    "who.a4t": "Страховые компании", "who.a4": "Предстраховой осмотр и фотофиксация убытков без выезда агента.",
    "price.eyebrow": "Тарифы", "price.title": "платите за машины, а не за подписку «на всякий случай»",
    "price.sub": "Цены предварительные: на время раннего доступа фиксируем их на первый год.",
    "price.sum": "сум", "price.from": "от", "price.perCar": "сум / машина в месяц", "price.tag": "популярный", "price.custom": "по запросу",
    "price.p1n": "Пилот", "price.p1d": "чтобы попробовать на своём автопарке",
    "price.p1f1": "До 10 машин, 1 месяц", "price.p1f2": "Все функции бота и кабинета", "price.p1f3": "Помощь с подключением", "price.p1c": "Начать пилот",
    "price.p2n": "Автопарк", "price.p2d": "для таксопарков и служб доставки",
    "price.p2f1": "Без ограничения по фото", "price.p2f2": "ИИ-оценка повреждений", "price.p2f3": "Отчёты в Excel и аналитика", "price.p2f4": "Поддержка в Telegram", "price.p2c": "Подключить",
    "price.p3d": "для страховых, лизинга и крупных парков",
    "price.p3f1": "Установка на ваших серверах", "price.p3f2": "Хранение данных в Узбекистане", "price.p3f3": "Интеграции и SLA", "price.p3c": "Связаться",
    "calc.eyebrow": "Калькулятор", "calc.title": "сколько времени вы сэкономите",
    "calc.cars": "Машин в автопарке", "calc.checks": "Проверок в неделю на машину", "calc.minutes": "Минут на ручную проверку",
    "calc.result": "Ваш результат", "calc.hours": "часов в месяц", "calc.hoursHint": "столько менеджер сейчас тратит на ручные проверки",
    "calc.checksMonth": "Проверок в месяц", "calc.price": "Diesel, тариф «Автопарк»",
    "faq.title": "частые вопросы",
    "faq.q1": "Нужно ли водителям ставить приложение?", "faq.a1": "Нет. Всё работает через Telegram-бота: водитель отправляет фото так же, как в обычный чат.",
    "faq.q2": "Какие номера распознаются?", "faq.a2": "Узбекские номера физлиц (01 A 123 BC) и юрлиц (01 123 ABC) всех 14 регионов, а также российские.",
    "faq.q3": "Как определяются повреждения?", "faq.a3": "Локальная нейросеть ищет вмятины, царапины, трещины, разбитые стёкла и фары, спущенные колёса. Спорные случаи дополнительно проверяет большая модель ИИ, а окончательное решение остаётся за менеджером.",
    "faq.q4": "Где хранятся фото?", "faq.a4": "В тарифе Enterprise Diesel устанавливается на ваши серверы, и данные не покидают вашу инфраструктуру.",
    "faq.q5": "Сколько длится подключение?", "faq.a5": "Создать автопарк и пригласить водителей можно за один вечер. На пилоте помогаем лично.",
    "final.title": "Готовы навести порядок в автопарке?", "final.sub": "Расскажите о своём автопарке — подключим пилот и покажем Diesel на ваших машинах.",
    "footer.about": "ИИ-осмотр автопарка в Telegram. Сделано в Ташкенте.", "footer.contacts": "Контакты", "footer.product": "Продукт",
    "footer.admin": "Кабинет администратора", "footer.driver": "Кабинет водителя",
    "contact.bot": "Telegram-бот", "contact.tg": "Telegram", "contact.email": "Email", "contact.phone": "Телефон",
    "contact.soon": "Контакты скоро появятся",
    "unit.sum": "сум",
    "meta.title": "Diesel — ИИ-осмотр автопарка в Telegram",
  },

  uz: {
    "nav.how": "Qanday ishlaydi", "nav.features": "Imkoniyatlar", "nav.pricing": "Tariflar", "nav.contacts": "Kontaktlar", "nav.login": "Kirish",
    "hero.pill": "Toshkentda pilot · erta kirish",
    "hero.t1": "Diesel —", "hero.t2": "avtopark uchun SI‑ko‘rik", "hero.t3": "to‘g‘ridan-to‘g‘ri Telegramda",
    "hero.lede": "Haydovchi mashina suratini botga yuboradi — Diesel raqamni o‘qiydi, shikastlarni topadi va qora ro‘yxat bilan solishtiradi. Menejer faqat e’tibor talab qiladigan holatlarni ko‘radi.",
    "hero.b1t": "Ilovasiz", "hero.b1": "haydovchilar o‘zlarida bor Telegramda ishlaydi. Ulanish — bir kechada.",
    "hero.b2t": "O‘zbek raqamlari", "hero.b2": "01 A 123 BC va 01 123 ABC, barcha 14 hudud. Raqam mos kelmasa — mashina tekshiruvga yuboriladi.",
    "hero.b3t": "Shikastlar va tarix", "hero.b3": "botiqlar, tirnalishlar, oyna, faralar, g‘ildiraklar. Har bir surat sanasi bilan — haydovchilar bilan kamroq bahs.",
    "hero.note": "Pilot bepul. Hech qanday uskuna yoki ilova kerak emas.",
    "cta.primary": "Avtoparkni ulash", "cta.login": "Kabinetga kirish",
    "demo.status": "bot · onlayn", "demo.foot": "suratni tekshirish", "demo.replay": "takrorlash",
    "demo.m1": "📷 Smena oldidan surat", "demo.m2": "Raqam aniqlandi", "demo.m3": "ro‘yxatdagi raqamga mos",
    "demo.m4": "Shikast: orqa bamperda tirnalish", "demo.m4s": "kichik",
    "demo.m5": "Holat: ACTIVE — yo‘lga chiqish mumkin ✅", "demo.m6": "Yaxshi smena!",
    "stats.s1": "turdagi shikast", "stats.s2": "O‘zbekiston hududi", "stats.s3": "o‘rnatiladigan ilova", "stats.s4": "bot suratlarni qabul qiladi",
    "why.eyebrow": "Avtoparkka nima uchun kerak", "why.title": "chatlardagi tartibsizliksiz ko‘rik",
    "why.sub": "Bugun haydovchilar suratlarni umumiy chatga yuboradi, menejer esa yuzlab xabarlarni varaqlab, raqamlarni ko‘z bilan solishtiradi. Diesel bu ishni o‘z zimmasiga oladi:",
    "why.c1t": "bir daqiqada ko‘rik", "why.c1": "Surat → raqam, shikastlar va holat avtomatik. Menejer faqat bahsli holatlarni ko‘radi.",
    "why.c2t": "har bir mashina tarixi", "why.c2": "Barcha suratlar sana va haydovchi bilan. Botiq qachon paydo bo‘lgani va rulda kim bo‘lgani ko‘rinadi.",
    "why.c3t": "qora ro‘yxat", "why.c3": "Bloklangan raqam darhol belgilanadi, admin Telegramda xabar oladi.",
    "how.eyebrow": "Qanday ishlaydi", "how.title": "to‘rt qadam — va avtopark nazoratda",
    "how.s1t": "Avtopark yarating", "how.s1": "Botga /start yozing va nomini kiriting — siz administratorsiz.",
    "how.s2t": "Haydovchilarni taklif qiling", "how.s2": "Bot taklif havolasini beradi. Haydovchi mashina raqami va modelini ro‘yxatdan o‘tkazadi.",
    "how.s3t": "Haydovchilar surat yuboradi", "how.s3": "Smena oldidan yoki eslatma bo‘yicha. SI raqam, shikastlar va qora ro‘yxatni tekshiradi.",
    "how.s4t": "Siz asosiysini ko‘rasiz", "how.s4": "Muammolar haqida Telegramda xabar, tarix va Excel hisobotlar — veb-kabinetda.",
    "cmp.eyebrow": "Taqqoslash", "cmp.title": "suratli chat va Diesel", "cmp.old": "Umumiy chatdagi suratlar", "cmp.new": "Diesel",
    "cmp.r1a": "Raqam ko‘z bilan solishtiriladi", "cmp.r1b": "Raqamni SI o‘qiydi va ro‘yxat bilan solishtiradi",
    "cmp.r2a": "Shikastlar kech payqaladi", "cmp.r2b": "Har bir surat 6 turdagi shikastga tekshiriladi",
    "cmp.r3a": "Suratlar yozishmalarda yo‘qoladi", "cmp.r3b": "Har bir mashina va haydovchi bo‘yicha tarix",
    "cmp.r4a": "Qora ro‘yxat menejer xotirasida", "cmp.r4b": "Bloklash va tezkor xabarnoma",
    "cmp.r5a": "Hisobotlar qo‘lda yig‘iladi", "cmp.r5b": "Bir bosishda Excelga eksport",
    "who.eyebrow": "Kimlar uchun", "who.title": "mashinasi ko‘p bo‘lgan har bir kishi uchun",
    "who.a1t": "Taksoparklar", "who.a1": "Smena oldidan har kungi ko‘rik va mashina almashtirilishini nazorat qilish.",
    "who.a2t": "Yetkazib berish", "who.a2": "Kuryer mashinalari va furgonlar: holati va haydovchi javobgarligi.",
    "who.a3t": "Lizing va ijara", "who.a3": "Berish va qaytarishda surat — ta’mir uchun kim to‘lashi aniq.",
    "who.a4t": "Sug‘urta kompaniyalari", "who.a4": "Agent chiqmasdan sug‘urta oldidan ko‘rik va zararni suratga olish.",
    "price.eyebrow": "Tariflar", "price.title": "«har ehtimolga qarshi» obuna uchun emas, mashinalar uchun to‘lang",
    "price.sub": "Narxlar dastlabki: erta kirish davrida ularni birinchi yilga qotiramiz.",
    "price.sum": "so‘m", "price.from": "", "price.perCar": "so‘mdan / mashina, oyiga", "price.tag": "ommabop", "price.custom": "kelishuv bo‘yicha",
    "price.p1n": "Pilot", "price.p1d": "o‘z avtoparkingizda sinab ko‘rish uchun",
    "price.p1f1": "10 tagacha mashina, 1 oy", "price.p1f2": "Bot va kabinetning barcha funksiyalari", "price.p1f3": "Ulanishda yordam", "price.p1c": "Pilotni boshlash",
    "price.p2n": "Avtopark", "price.p2d": "taksoparklar va yetkazib berish xizmatlari uchun",
    "price.p2f1": "Suratlar soni cheklanmagan", "price.p2f2": "Shikastlarni SI bilan baholash", "price.p2f3": "Excel hisobotlar va tahlil", "price.p2f4": "Telegramda yordam", "price.p2c": "Ulash",
    "price.p3d": "sug‘urta, lizing va yirik parklar uchun",
    "price.p3f1": "Sizning serverlaringizga o‘rnatish", "price.p3f2": "Ma’lumotlarni O‘zbekistonda saqlash", "price.p3f3": "Integratsiyalar va SLA", "price.p3c": "Bog‘lanish",
    "calc.eyebrow": "Kalkulyator", "calc.title": "qancha vaqt tejaysiz",
    "calc.cars": "Avtoparkdagi mashinalar", "calc.checks": "Bir mashinaga haftalik tekshiruvlar", "calc.minutes": "Qo‘lda tekshirishga daqiqa",
    "calc.result": "Natijangiz", "calc.hours": "soat oyiga", "calc.hoursHint": "menejer hozir qo‘lda tekshirishga shuncha vaqt sarflaydi",
    "calc.checksMonth": "Oyiga tekshiruvlar", "calc.price": "Diesel, «Avtopark» tarifi",
    "faq.title": "ko‘p beriladigan savollar",
    "faq.q1": "Haydovchilarga ilova o‘rnatish kerakmi?", "faq.a1": "Yo‘q. Hammasi Telegram-bot orqali ishlaydi: haydovchi suratni oddiy chatdagidek yuboradi.",
    "faq.q2": "Qaysi raqamlar aniqlanadi?", "faq.a2": "Barcha 14 hududdagi jismoniy (01 A 123 BC) va yuridik shaxslar (01 123 ABC) raqamlari, shuningdek Rossiya raqamlari.",
    "faq.q3": "Shikastlar qanday aniqlanadi?", "faq.a3": "Lokal neyrotarmoq botiqlar, tirnalishlar, yoriqlar, singan oyna va faralar, puchaygan g‘ildiraklarni qidiradi. Bahsli holatlarni katta SI modeli qo‘shimcha tekshiradi, yakuniy qaror esa menejerda qoladi.",
    "faq.q4": "Suratlar qayerda saqlanadi?", "faq.a4": "Enterprise tarifida Diesel sizning serverlaringizga o‘rnatiladi va ma’lumotlar infratuzilmangizdan chiqmaydi.",
    "faq.q5": "Ulanish qancha vaqt oladi?", "faq.a5": "Avtopark yaratish va haydovchilarni taklif qilish bir kechada mumkin. Pilotda shaxsan yordam beramiz.",
    "final.title": "Avtoparkda tartib o‘rnatishga tayyormisiz?", "final.sub": "Avtoparkingiz haqida aytib bering — pilotni ulaymiz va Diesel’ni mashinalaringizda ko‘rsatamiz.",
    "footer.about": "Telegramda avtopark uchun SI‑ko‘rik. Toshkentda yaratilgan.", "footer.contacts": "Kontaktlar", "footer.product": "Mahsulot",
    "footer.admin": "Administrator kabineti", "footer.driver": "Haydovchi kabineti",
    "contact.bot": "Telegram-bot", "contact.tg": "Telegram", "contact.email": "Email", "contact.phone": "Telefon",
    "contact.soon": "Kontaktlar tez orada paydo bo‘ladi",
    "unit.sum": "so‘m",
    "meta.title": "Diesel — Telegramda avtopark uchun SI‑ko‘rik",
  },

  en: {
    "nav.how": "How it works", "nav.features": "Features", "nav.pricing": "Pricing", "nav.contacts": "Contacts", "nav.login": "Log in",
    "hero.pill": "Pilot in Tashkent · early access",
    "hero.t1": "Diesel is", "hero.t2": "AI fleet inspection", "hero.t3": "right inside Telegram",
    "hero.lede": "Drivers send a photo of the car to the bot — Diesel reads the plate, looks for damage and checks the blacklist. Managers only see what needs attention.",
    "hero.b1t": "No apps", "hero.b1": "drivers use the Telegram they already have. Set up in one evening.",
    "hero.b2t": "Uzbek plates", "hero.b2": "01 A 123 BC and 01 123 ABC, all 14 regions. Plate mismatch — the car goes to review.",
    "hero.b3t": "Damage and history", "hero.b3": "dents, scratches, glass, lights, tires. Every photo is dated — fewer disputes with drivers.",
    "hero.note": "The pilot is free. No hardware, no apps.",
    "cta.primary": "Connect your fleet", "cta.login": "Open dashboard",
    "demo.status": "bot · online", "demo.foot": "photo check", "demo.replay": "replay",
    "demo.m1": "📷 Pre-shift photo", "demo.m2": "Plate recognized", "demo.m3": "matches registration",
    "demo.m4": "Damage: scratch on rear bumper", "demo.m4s": "minor",
    "demo.m5": "Status: ACTIVE — good to go ✅", "demo.m6": "Have a good shift!",
    "stats.s1": "damage types", "stats.s2": "regions of Uzbekistan", "stats.s3": "apps to install", "stats.s4": "bot accepts photos",
    "why.eyebrow": "Why fleets need it", "why.title": "inspections without chat chaos",
    "why.sub": "Today drivers post photos into a group chat and a manager scrolls through hundreds of messages, checking plates by eye. Diesel takes over the routine:",
    "why.c1t": "inspection in a minute", "why.c1": "Photo → plate, damage and status automatically. Managers only look at disputed cases.",
    "why.c2t": "history of every car", "why.c2": "Every photo with date and driver. You see when a dent appeared and who was driving.",
    "why.c3t": "blacklist", "why.c3": "A blocked plate is flagged instantly and admins get a Telegram alert.",
    "how.eyebrow": "How it works", "how.title": "four steps to a fleet under control",
    "how.s1t": "Create a fleet", "how.s1": "Send /start to the bot and enter a name — you're the admin.",
    "how.s2t": "Invite drivers", "how.s2": "The bot gives you an invite link. Drivers register their plate and car model.",
    "how.s3t": "Drivers send photos", "how.s3": "Before a shift or on reminder. AI checks the plate, damage and blacklist.",
    "how.s4t": "You see what matters", "how.s4": "Alerts in Telegram, history and Excel reports in the web dashboard.",
    "cmp.eyebrow": "Comparison", "cmp.title": "photo chat vs Diesel", "cmp.old": "Photos in a group chat", "cmp.new": "Diesel",
    "cmp.r1a": "Plates checked by eye", "cmp.r1b": "AI reads the plate and matches the registration",
    "cmp.r2a": "Damage noticed too late", "cmp.r2b": "Every photo checked for 6 damage types",
    "cmp.r3a": "Photos lost in the chat", "cmp.r3b": "History per car and driver",
    "cmp.r4a": "Blacklist lives in a manager's head", "cmp.r4b": "Blocking with instant alerts",
    "cmp.r5a": "Reports compiled by hand", "cmp.r5b": "One-click Excel export",
    "who.eyebrow": "Who it's for", "who.title": "for anyone running lots of cars",
    "who.a1t": "Taxi fleets", "who.a1": "Daily pre-shift checks and control over swapped cars.",
    "who.a2t": "Delivery", "who.a2": "Courier cars and vans: condition and driver accountability.",
    "who.a3t": "Leasing & rental", "who.a3": "Photos at hand-over and return — clear who pays for repairs.",
    "who.a4t": "Insurers", "who.a4": "Pre-insurance inspection and claim photos without sending an agent.",
    "price.eyebrow": "Pricing", "price.title": "pay for cars, not for a just-in-case subscription",
    "price.sub": "Prices are preliminary: during early access we lock them in for your first year.",
    "price.sum": "so'm", "price.from": "from", "price.perCar": "so'm / car per month", "price.tag": "popular", "price.custom": "custom",
    "price.p1n": "Pilot", "price.p1d": "to try it on your own fleet",
    "price.p1f1": "Up to 10 cars, 1 month", "price.p1f2": "All bot and dashboard features", "price.p1f3": "Help with setup", "price.p1c": "Start pilot",
    "price.p2n": "Fleet", "price.p2d": "for taxi fleets and delivery services",
    "price.p2f1": "Unlimited photos", "price.p2f2": "AI damage assessment", "price.p2f3": "Excel reports and analytics", "price.p2f4": "Support in Telegram", "price.p2c": "Connect",
    "price.p3d": "for insurers, leasing and large fleets",
    "price.p3f1": "Installed on your servers", "price.p3f2": "Data stored in Uzbekistan", "price.p3f3": "Integrations and SLA", "price.p3c": "Contact us",
    "calc.eyebrow": "Calculator", "calc.title": "how much time you'll save",
    "calc.cars": "Cars in the fleet", "calc.checks": "Checks per car per week", "calc.minutes": "Minutes per manual check",
    "calc.result": "Your result", "calc.hours": "hours a month", "calc.hoursHint": "what a manager now spends on manual checks",
    "calc.checksMonth": "Checks per month", "calc.price": "Diesel, Fleet plan",
    "faq.title": "frequently asked questions",
    "faq.q1": "Do drivers need to install an app?", "faq.a1": "No. Everything works through a Telegram bot: drivers send photos like in any chat.",
    "faq.q2": "Which plates are recognized?", "faq.a2": "Uzbek private (01 A 123 BC) and company (01 123 ABC) plates from all 14 regions, plus Russian plates.",
    "faq.q3": "How is damage detected?", "faq.a3": "A local neural network looks for dents, scratches, cracks, broken glass and lights, and flat tires. Unclear cases are double-checked by a larger AI model, and the final call stays with the manager.",
    "faq.q4": "Where are photos stored?", "faq.a4": "On the Enterprise plan Diesel is installed on your servers, so data never leaves your infrastructure.",
    "faq.q5": "How long does setup take?", "faq.a5": "Creating a fleet and inviting drivers takes one evening. During the pilot we help in person.",
    "final.title": "Ready to bring order to your fleet?", "final.sub": "Tell us about your fleet — we'll set up a pilot and show Diesel on your cars.",
    "footer.about": "AI fleet inspection in Telegram. Made in Tashkent.", "footer.contacts": "Contacts", "footer.product": "Product",
    "footer.admin": "Admin dashboard", "footer.driver": "Driver portal",
    "contact.bot": "Telegram bot", "contact.tg": "Telegram", "contact.email": "Email", "contact.phone": "Phone",
    "contact.soon": "Contacts coming soon",
    "unit.sum": "so'm",
    "meta.title": "Diesel — AI fleet inspection in Telegram",
  },
};

const LANGS = ["ru", "uz", "en"];

function getLang() {
  try {
    const saved = localStorage.getItem("fleetscan.locale");
    if (LANGS.includes(saved)) return saved;
  } catch { /* storage unavailable */ }
  const nav = (navigator.language || "ru").slice(0, 2);
  return LANGS.includes(nav) ? nav : "ru";
}

let lang = getLang();
const t = (key) => T[lang][key] ?? T.ru[key] ?? key;

function fmt(n) {
  return Math.round(n).toLocaleString(lang === "en" ? "en-US" : "ru-RU").replace(/,/g, " ");
}

function applyLang() {
  document.documentElement.lang = lang;
  document.title = t("meta.title");
  document.querySelectorAll("[data-t]").forEach((el) => {
    const value = t(el.dataset.t);
    el.textContent = value;
    el.hidden = value === "";
  });
  document.querySelectorAll(".lang-switch button").forEach((b) => b.classList.toggle("is-active", b.dataset.lang === lang));
  document.getElementById("pricePerCar").textContent = fmt(PRICE_PER_CAR);
  renderContacts();
  updateCalc();
  playDemo();
}

// ---- theme ----
function setTheme(theme) {
  if (theme === "dark") document.documentElement.setAttribute("data-theme", "dark");
  else document.documentElement.removeAttribute("data-theme");
  try { localStorage.setItem("fleetscan.theme", theme); } catch { /* ignore */ }
}
document.getElementById("themeToggle").addEventListener("click", () => {
  setTheme(document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark");
});

document.querySelectorAll(".lang-switch button").forEach((b) => {
  b.addEventListener("click", () => {
    lang = b.dataset.lang;
    try { localStorage.setItem("fleetscan.locale", lang); } catch { /* ignore */ }
    applyLang();
  });
});

// ---- contacts ----
function renderContacts() {
  const items = [];
  if (CONTACTS.telegramBot) items.push([t("contact.bot"), `https://t.me/${CONTACTS.telegramBot}`, `@${CONTACTS.telegramBot}`]);
  if (CONTACTS.telegram) items.push([t("contact.tg"), `https://t.me/${CONTACTS.telegram}`, `@${CONTACTS.telegram}`]);
  if (CONTACTS.email) items.push([t("contact.email"), `mailto:${CONTACTS.email}`, CONTACTS.email]);
  if (CONTACTS.phone) items.push([t("contact.phone"), `tel:${CONTACTS.phone.replace(/\s/g, "")}`, CONTACTS.phone]);

  const list = document.getElementById("contactList");
  list.innerHTML = "";
  if (!items.length) {
    const li = document.createElement("li");
    li.className = "hint";
    li.textContent = t("contact.soon");
    list.appendChild(li);
  }
  for (const [label, href, text] of items) {
    const li = document.createElement("li");
    li.innerHTML = `<span class="label"></span><a></a>`;
    li.querySelector(".label").textContent = label;
    const a = li.querySelector("a");
    a.href = href;
    a.textContent = text;
    if (href.startsWith("https://")) { a.target = "_blank"; a.rel = "noopener"; }
    list.appendChild(li);
  }

  // Primary CTA opens the bot when it is configured.
  if (CONTACTS.telegramBot) {
    document.querySelectorAll('a[href="#contacts"][data-t="cta.primary"], #ctaPrimary').forEach((a) => {
      a.href = `https://t.me/${CONTACTS.telegramBot}?start`;
      a.target = "_blank";
      a.rel = "noopener";
    });
  }
}

// ---- calculator ----
const $ = (id) => document.getElementById(id);
function updateCalc() {
  const cars = +$("cars").value;
  const checks = +$("checks").value;
  const minutes = +$("minutes").value;
  const checksMonth = cars * checks * 4.33;
  $("carsOut").textContent = cars;
  $("checksOut").textContent = checks;
  $("minutesOut").textContent = minutes;
  $("hoursSaved").textContent = fmt((checksMonth * minutes) / 60);
  $("checksMonth").textContent = fmt(checksMonth);
  $("dieselPrice").textContent = `${fmt(cars * PRICE_PER_CAR)} ${t("unit.sum")}`;
}
["cars", "checks", "minutes"].forEach((id) => $(id).addEventListener("input", updateCalc));

// ---- chat demo ----
const CAR_SVG = `
<svg viewBox="0 0 200 120" xmlns="http://www.w3.org/2000/svg">
  <rect width="200" height="120" fill="#cfe3d6"/>
  <rect y="88" width="200" height="32" fill="#9fb8a8"/>
  <path d="M30 80 L45 52 Q50 44 60 44 L140 44 Q150 44 155 52 L170 80 Z" fill="#f4f6f5"/>
  <rect x="22" y="70" width="156" height="22" rx="8" fill="#ffffff"/>
  <path d="M58 50 L70 50 L70 70 L50 70 Z M78 50 L122 50 L122 70 L78 70 Z M130 50 L142 50 L150 70 L130 70 Z" fill="#5b7a8c" opacity=".75"/>
  <circle cx="55" cy="92" r="12" fill="#2b2f2d"/><circle cx="145" cy="92" r="12" fill="#2b2f2d"/>
  <circle cx="55" cy="92" r="5" fill="#9aa39e"/><circle cx="145" cy="92" r="5" fill="#9aa39e"/>
  <rect x="80" y="76" width="40" height="11" rx="2" fill="#fff" stroke="#2b2f2d" stroke-width="1"/>
  <text x="100" y="84.5" font-family="monospace" font-size="7" text-anchor="middle" fill="#1b1b1b">01 A 123 BC</text>
  <path d="M150 74 l10 3" stroke="#c2372f" stroke-width="1.5" stroke-linecap="round"/>
</svg>`;

let demoTimers = [];
function playDemo() {
  demoTimers.forEach(clearTimeout);
  demoTimers = [];
  const body = $("chatBody");
  body.innerHTML = "";
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const steps = [
    ["out photo", CAR_SVG],
    ["out", t("demo.m1")],
    ["typing"],
    ["in", `🔎 ${t("demo.m2")}: <span class="mono">01 A 123 BC</span><br><span class="ok">✓ ${t("demo.m3")}</span>`],
    ["typing"],
    ["in", `⚠️ ${t("demo.m4")} — <span class="warn">${t("demo.m4s")}</span>`],
    ["in", `<b>${t("demo.m5")}</b>`],
    ["in", t("demo.m6")],
  ];

  let delay = 300;
  let typingEl = null;
  for (const [kind, html] of steps) {
    const at = reduced ? 0 : delay;
    demoTimers.push(setTimeout(() => {
      if (typingEl) { typingEl.remove(); typingEl = null; }
      const el = document.createElement("div");
      if (kind === "typing") {
        el.className = "msg in typing";
        el.innerHTML = "<i></i><i></i><i></i>";
        typingEl = el;
      } else {
        el.className = `msg ${kind}`;
        el.innerHTML = html;
      }
      body.appendChild(el);
    }, at));
    delay += kind === "typing" ? 1100 : 700;
  }
}
$("chatReplay").addEventListener("click", playDemo);

applyLang();
