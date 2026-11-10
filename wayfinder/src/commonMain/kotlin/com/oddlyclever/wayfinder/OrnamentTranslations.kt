package com.oddlyclever.wayfinder

// wayfinder's own words for its ornaments, one entry per language: a language code, or a code and region where the
// region changes the writing, as Taiwan's traditional Chinese does: 53 languages, Chinese in both scripts. Inuktitut
// and the other Canadian Indigenous languages aughtone-types knows, such as the Cree and Ojibwe added in 4.2.0, fall
// back to English for now, until translations can be checked by speakers, as does any other language. Kept as Kotlin
// rather than Compose resources for two reasons: on the web, resources load asynchronously, so a label would be empty
// until its file arrived; and resources pick a language for themselves, where these follow aughtone-types'
// Locale.current, as the scale bar's number does. Resource files suit translators better; move to them if the
// translations start coming from a translation service. A language is added with all eight fields. Where a language has no settled short form for the points of the compass, its
// points are the international N, NE, E and so on.

internal val EnglishOrnamentWords: OrnamentWords get() = OrnamentTranslations.getValue("en")

internal val OrnamentTranslations: Map<String, OrnamentWords> by lazy {
    mapOf(
        // English
        "en" to OrnamentWords(
            zoomIn = "Zoom in",
            zoomOut = "Zoom out",
            north = "North",
            compass = "Compass",
            credits = "Map credits",
            northUp = "North up: tap for course up",
            courseUp = "Course up: tap for north up",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Afrikaans
        "af" to OrnamentWords(
            zoomIn = "Zoem in",
            zoomOut = "Zoem uit",
            north = "Noord",
            compass = "Kompas",
            credits = "Kaartbronne",
            northUp = "Noord bo: tik vir koers bo",
            courseUp = "Koers bo: tik vir noord bo",
            compassPoints = listOf("N", "NO", "O", "SO", "S", "SW", "W", "NW"),
        ),
        // Dutch
        "nl" to OrnamentWords(
            zoomIn = "Inzoomen",
            zoomOut = "Uitzoomen",
            north = "Noorden",
            compass = "Kompas",
            credits = "Kaartbronnen",
            northUp = "Noorden boven: tik voor rijrichting boven",
            courseUp = "Rijrichting boven: tik voor noorden boven",
            compassPoints = listOf("N", "NO", "O", "ZO", "Z", "ZW", "W", "NW"),
        ),
        // German
        "de" to OrnamentWords(
            zoomIn = "Vergrößern",
            zoomOut = "Verkleinern",
            north = "Norden",
            compass = "Kompass",
            credits = "Kartenquellen",
            northUp = "Norden oben: tippen für Fahrtrichtung oben",
            courseUp = "Fahrtrichtung oben: tippen für Norden oben",
            compassPoints = listOf("N", "NO", "O", "SO", "S", "SW", "W", "NW"),
        ),
        // Danish
        "da" to OrnamentWords(
            zoomIn = "Zoom ind",
            zoomOut = "Zoom ud",
            north = "Nord",
            compass = "Kompas",
            credits = "Kortkilder",
            northUp = "Nord opad: tryk for kørselsretning opad",
            courseUp = "Kørselsretning opad: tryk for nord opad",
            compassPoints = listOf("N", "NØ", "Ø", "SØ", "S", "SV", "V", "NV"),
        ),
        // Norwegian Bokmål
        "nb" to OrnamentWords(
            zoomIn = "Zoom inn",
            zoomOut = "Zoom ut",
            north = "Nord",
            compass = "Kompass",
            credits = "Kartkilder",
            northUp = "Nord opp: trykk for kjøreretning opp",
            courseUp = "Kjøreretning opp: trykk for nord opp",
            compassPoints = listOf("N", "NØ", "Ø", "SØ", "S", "SV", "V", "NV"),
        ),
        // Norwegian Nynorsk
        "nn" to OrnamentWords(
            zoomIn = "Zoom inn",
            zoomOut = "Zoom ut",
            north = "Nord",
            compass = "Kompass",
            credits = "Kartkjelder",
            northUp = "Nord opp: trykk for køyreretning opp",
            courseUp = "Køyreretning opp: trykk for nord opp",
            compassPoints = listOf("N", "NØ", "Ø", "SØ", "S", "SV", "V", "NV"),
        ),
        // Swedish
        "sv" to OrnamentWords(
            zoomIn = "Zooma in",
            zoomOut = "Zooma ut",
            north = "Norr",
            compass = "Kompass",
            credits = "Kartkällor",
            northUp = "Norr uppåt: tryck för färdriktning uppåt",
            courseUp = "Färdriktning uppåt: tryck för norr uppåt",
            compassPoints = listOf("N", "NO", "O", "SO", "S", "SV", "V", "NV"),
        ),
        // Icelandic
        "is" to OrnamentWords(
            zoomIn = "Auka aðdrátt",
            zoomOut = "Minnka aðdrátt",
            north = "Norður",
            compass = "Áttaviti",
            credits = "Heimildir korts",
            northUp = "Norður upp: ýttu fyrir ferðastefnu upp",
            courseUp = "Ferðastefna upp: ýttu fyrir norður upp",
            compassPoints = listOf("N", "NA", "A", "SA", "S", "SV", "V", "NV"),
        ),
        // French
        "fr" to OrnamentWords(
            zoomIn = "Zoom avant",
            zoomOut = "Zoom arrière",
            north = "Nord",
            compass = "Boussole",
            credits = "Crédits de la carte",
            northUp = "Nord en haut : touchez pour orienter selon le cap",
            courseUp = "Orientée selon le cap : touchez pour le nord en haut",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO"),
        ),
        // Spanish
        "es" to OrnamentWords(
            zoomIn = "Acercar",
            zoomOut = "Alejar",
            north = "Norte",
            compass = "Brújula",
            credits = "Créditos del mapa",
            northUp = "Norte arriba: toca para orientar según el rumbo",
            courseUp = "Orientado según el rumbo: toca para norte arriba",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO"),
        ),
        // Italian
        "it" to OrnamentWords(
            zoomIn = "Ingrandisci",
            zoomOut = "Riduci",
            north = "Nord",
            compass = "Bussola",
            credits = "Crediti della mappa",
            northUp = "Nord in alto: tocca per orientare secondo la direzione",
            courseUp = "Orientata secondo la direzione: tocca per il nord in alto",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO"),
        ),
        // Portuguese
        "pt" to OrnamentWords(
            zoomIn = "Aumentar zoom",
            zoomOut = "Diminuir zoom",
            north = "Norte",
            compass = "Bússola",
            credits = "Créditos do mapa",
            northUp = "Norte para cima: toque para orientar pela direção",
            courseUp = "Orientado pela direção: toque para norte para cima",
            compassPoints = listOf("N", "NE", "L", "SE", "S", "SO", "O", "NO"),
        ),
        // Catalan
        "ca" to OrnamentWords(
            zoomIn = "Apropa",
            zoomOut = "Allunya",
            north = "Nord",
            compass = "Brúixola",
            credits = "Crèdits del mapa",
            northUp = "Nord amunt: toca per orientar segons el rumb",
            courseUp = "Orientat segons el rumb: toca per al nord amunt",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO"),
        ),
        // Galician
        "gl" to OrnamentWords(
            zoomIn = "Achegar",
            zoomOut = "Afastar",
            north = "Norte",
            compass = "Compás",
            credits = "Créditos do mapa",
            northUp = "Norte arriba: toca para orientar segundo o rumbo",
            courseUp = "Orientado segundo o rumbo: toca para o norte arriba",
            compassPoints = listOf("N", "NL", "L", "SL", "S", "SO", "O", "NO"),
        ),
        // Romanian
        "ro" to OrnamentWords(
            zoomIn = "Mărește",
            zoomOut = "Micșorează",
            north = "Nord",
            compass = "Busolă",
            credits = "Sursele hărții",
            northUp = "Nordul sus: atinge pentru direcția de mers sus",
            courseUp = "Direcția de mers sus: atinge pentru nordul sus",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SV", "V", "NV"),
        ),
        // Russian
        "ru" to OrnamentWords(
            zoomIn = "Увеличить",
            zoomOut = "Уменьшить",
            north = "Север",
            compass = "Компас",
            credits = "Источники карты",
            northUp = "Север вверху: нажмите, чтобы вверху было направление движения",
            courseUp = "Направление движения вверху: нажмите, чтобы вверху был север",
            compassPoints = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"),
        ),
        // Ukrainian
        "uk" to OrnamentWords(
            zoomIn = "Збільшити",
            zoomOut = "Зменшити",
            north = "Північ",
            compass = "Компас",
            credits = "Джерела карти",
            northUp = "Північ угорі: торкніться, щоб угорі був напрямок руху",
            courseUp = "Напрямок руху вгорі: торкніться, щоб угорі була північ",
            compassPoints = listOf("Пн", "ПнСх", "Сх", "ПдСх", "Пд", "ПдЗх", "Зх", "ПнЗх"),
        ),
        // Belarusian
        "be" to OrnamentWords(
            zoomIn = "Павялічыць",
            zoomOut = "Паменшыць",
            north = "Поўнач",
            compass = "Компас",
            credits = "Крыніцы карты",
            northUp = "Поўнач уверсе: націсніце, каб уверсе быў кірунак руху",
            courseUp = "Кірунак руху ўверсе: націсніце, каб уверсе была поўнач",
            compassPoints = listOf("Пн", "ПнУ", "У", "ПдУ", "Пд", "ПдЗ", "З", "ПнЗ"),
        ),
        // Polish
        "pl" to OrnamentWords(
            zoomIn = "Powiększ",
            zoomOut = "Pomniejsz",
            north = "Północ",
            compass = "Kompas",
            credits = "Źródła mapy",
            northUp = "Północ u góry: dotknij, aby u góry był kierunek jazdy",
            courseUp = "Kierunek jazdy u góry: dotknij, aby u góry była północ",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Czech
        "cs" to OrnamentWords(
            zoomIn = "Přiblížit",
            zoomOut = "Oddálit",
            north = "Sever",
            compass = "Kompas",
            credits = "Zdroje mapy",
            northUp = "Sever nahoře: klepněte pro směr jízdy nahoře",
            courseUp = "Směr jízdy nahoře: klepněte pro sever nahoře",
            compassPoints = listOf("S", "SV", "V", "JV", "J", "JZ", "Z", "SZ"),
        ),
        // Slovak
        "sk" to OrnamentWords(
            zoomIn = "Priblížiť",
            zoomOut = "Oddialiť",
            north = "Sever",
            compass = "Kompas",
            credits = "Zdroje mapy",
            northUp = "Sever hore: ťuknite pre smer jazdy hore",
            courseUp = "Smer jazdy hore: ťuknite pre sever hore",
            compassPoints = listOf("S", "SV", "V", "JV", "J", "JZ", "Z", "SZ"),
        ),
        // Bulgarian
        "bg" to OrnamentWords(
            zoomIn = "Увеличаване",
            zoomOut = "Намаляване",
            north = "Север",
            compass = "Компас",
            credits = "Източници на картата",
            northUp = "Север горе: докоснете за посоката на движение горе",
            courseUp = "Посоката на движение горе: докоснете за север горе",
            compassPoints = listOf("С", "СИ", "И", "ЮИ", "Ю", "ЮЗ", "З", "СЗ"),
        ),
        // Croatian
        "hr" to OrnamentWords(
            zoomIn = "Povećaj",
            zoomOut = "Smanji",
            north = "Sjever",
            compass = "Kompas",
            credits = "Izvori karte",
            northUp = "Sjever gore: dodirnite za smjer kretanja gore",
            courseUp = "Smjer kretanja gore: dodirnite za sjever gore",
            compassPoints = listOf("S", "SI", "I", "JI", "J", "JZ", "Z", "SZ"),
        ),
        // Serbian, in Cyrillic
        "sr" to OrnamentWords(
            zoomIn = "Увећај",
            zoomOut = "Умањи",
            north = "Север",
            compass = "Компас",
            credits = "Извори мапе",
            northUp = "Север горе: додирните за смер кретања горе",
            courseUp = "Смер кретања горе: додирните за север горе",
            compassPoints = listOf("С", "СИ", "И", "ЈИ", "Ј", "ЈЗ", "З", "СЗ"),
        ),
        // Macedonian
        "mk" to OrnamentWords(
            zoomIn = "Зголеми",
            zoomOut = "Намали",
            north = "Север",
            compass = "Компас",
            credits = "Извори на картата",
            northUp = "Север горе: допрете за насоката на движење горе",
            courseUp = "Насоката на движење горе: допрете за север горе",
            compassPoints = listOf("С", "СИ", "И", "ЈИ", "Ј", "ЈЗ", "З", "СЗ"),
        ),
        // Slovenian
        "sl" to OrnamentWords(
            zoomIn = "Povečaj",
            zoomOut = "Pomanjšaj",
            north = "Sever",
            compass = "Kompas",
            credits = "Viri zemljevida",
            northUp = "Sever zgoraj: tapnite za smer vožnje zgoraj",
            courseUp = "Smer vožnje zgoraj: tapnite za sever zgoraj",
            compassPoints = listOf("S", "SV", "V", "JV", "J", "JZ", "Z", "SZ"),
        ),
        // Lithuanian
        "lt" to OrnamentWords(
            zoomIn = "Priartinti",
            zoomOut = "Nutolinti",
            north = "Šiaurė",
            compass = "Kompasas",
            credits = "Žemėlapio šaltiniai",
            northUp = "Šiaurė viršuje: palieskite, kad viršuje būtų judėjimo kryptis",
            courseUp = "Judėjimo kryptis viršuje: palieskite, kad viršuje būtų šiaurė",
            compassPoints = listOf("Š", "ŠR", "R", "PR", "P", "PV", "V", "ŠV"),
        ),
        // Latvian
        "lv" to OrnamentWords(
            zoomIn = "Tuvināt",
            zoomOut = "Tālināt",
            north = "Ziemeļi",
            compass = "Kompass",
            credits = "Kartes avoti",
            northUp = "Ziemeļi augšā: pieskarieties, lai augšā būtu kustības virziens",
            courseUp = "Kustības virziens augšā: pieskarieties, lai augšā būtu ziemeļi",
            compassPoints = listOf("Z", "ZA", "A", "DA", "D", "DR", "R", "ZR"),
        ),
        // Japanese
        "ja" to OrnamentWords(
            zoomIn = "拡大",
            zoomOut = "縮小",
            north = "北",
            compass = "コンパス",
            credits = "地図のクレジット",
            northUp = "北が上: タップで進行方向を上にします",
            courseUp = "進行方向が上: タップで北を上にします",
            compassPoints = listOf("北", "北東", "東", "南東", "南", "南西", "西", "北西"),
        ),
        // Chinese, simplified
        "zh" to OrnamentWords(
            zoomIn = "放大",
            zoomOut = "缩小",
            north = "北",
            compass = "指南针",
            credits = "地图来源",
            northUp = "北向上：点按切换为行进方向向上",
            courseUp = "行进方向向上：点按切换为北向上",
            compassPoints = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北"),
        ),
        // Chinese, traditional
        "zh-TW" to OrnamentWords(
            zoomIn = "放大",
            zoomOut = "縮小",
            north = "北",
            compass = "指南針",
            credits = "地圖來源",
            northUp = "北方朝上：點按切換為行進方向朝上",
            courseUp = "行進方向朝上：點按切換為北方朝上",
            compassPoints = listOf("北", "東北", "東", "東南", "南", "西南", "西", "西北"),
        ),
        // Korean
        "ko" to OrnamentWords(
            zoomIn = "확대",
            zoomOut = "축소",
            north = "북쪽",
            compass = "나침반",
            credits = "지도 출처",
            northUp = "북쪽이 위: 탭하면 진행 방향이 위로 바뀝니다",
            courseUp = "진행 방향이 위: 탭하면 북쪽이 위로 바뀝니다",
            compassPoints = listOf("북", "북동", "동", "남동", "남", "남서", "서", "북서"),
        ),
        // Thai
        "th" to OrnamentWords(
            zoomIn = "ซูมเข้า",
            zoomOut = "ซูมออก",
            north = "ทิศเหนือ",
            compass = "เข็มทิศ",
            credits = "แหล่งที่มาของแผนที่",
            northUp = "ทิศเหนืออยู่ด้านบน: แตะเพื่อให้ทิศทางการเดินทางอยู่ด้านบน",
            courseUp = "ทิศทางการเดินทางอยู่ด้านบน: แตะเพื่อให้ทิศเหนืออยู่ด้านบน",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Vietnamese
        "vi" to OrnamentWords(
            zoomIn = "Phóng to",
            zoomOut = "Thu nhỏ",
            north = "Bắc",
            compass = "La bàn",
            credits = "Nguồn bản đồ",
            northUp = "Hướng bắc ở trên: nhấn để hướng di chuyển ở trên",
            courseUp = "Hướng di chuyển ở trên: nhấn để hướng bắc ở trên",
            compassPoints = listOf("B", "ĐB", "Đ", "ĐN", "N", "TN", "T", "TB"),
        ),
        // Indonesian
        "id" to OrnamentWords(
            zoomIn = "Perbesar",
            zoomOut = "Perkecil",
            north = "Utara",
            compass = "Kompas",
            credits = "Sumber peta",
            northUp = "Utara di atas: ketuk untuk arah perjalanan di atas",
            courseUp = "Arah perjalanan di atas: ketuk untuk utara di atas",
            compassPoints = listOf("U", "TL", "T", "TG", "S", "BD", "B", "BL"),
        ),
        // Malay
        "ms" to OrnamentWords(
            zoomIn = "Zum masuk",
            zoomOut = "Zum keluar",
            north = "Utara",
            compass = "Kompas",
            credits = "Sumber peta",
            northUp = "Utara di atas: ketik untuk arah perjalanan di atas",
            courseUp = "Arah perjalanan di atas: ketik untuk utara di atas",
            compassPoints = listOf("U", "TL", "T", "TG", "S", "BD", "B", "BL"),
        ),
        // Hindi
        "hi" to OrnamentWords(
            zoomIn = "ज़ूम इन करें",
            zoomOut = "ज़ूम आउट करें",
            north = "उत्तर",
            compass = "कम्पास",
            credits = "मानचित्र के स्रोत",
            northUp = "उत्तर ऊपर: यात्रा की दिशा ऊपर करने के लिए टैप करें",
            courseUp = "यात्रा की दिशा ऊपर: उत्तर ऊपर करने के लिए टैप करें",
            compassPoints = listOf("उ", "उपू", "पू", "दपू", "द", "दप", "प", "उप"),
        ),
        // Armenian
        "hy" to OrnamentWords(
            zoomIn = "Մեծացնել",
            zoomOut = "Փոքրացնել",
            north = "Հյուսիս",
            compass = "Կողմնացույց",
            credits = "Քարտեզի աղբյուրներ",
            northUp = "Հյուսիսը վերևում. հպեք, որ վերևում լինի շարժման ուղղությունը",
            courseUp = "Շարժման ուղղությունը վերևում. հպեք, որ վերևում լինի հյուսիսը",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Georgian
        "ka" to OrnamentWords(
            zoomIn = "გადიდება",
            zoomOut = "დაპატარავება",
            north = "ჩრდილოეთი",
            compass = "კომპასი",
            credits = "რუკის წყაროები",
            northUp = "ჩრდილოეთი ზემოთ: შეეხეთ, რომ ზემოთ იყოს მოძრაობის მიმართულება",
            courseUp = "მოძრაობის მიმართულება ზემოთ: შეეხეთ, რომ ზემოთ იყოს ჩრდილოეთი",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Kazakh
        "kk" to OrnamentWords(
            zoomIn = "Үлкейту",
            zoomOut = "Кішірейту",
            north = "Солтүстік",
            compass = "Компас",
            credits = "Карта дереккөздері",
            northUp = "Солтүстік жоғарыда: жоғарыда қозғалыс бағыты болуы үшін түртіңіз",
            courseUp = "Қозғалыс бағыты жоғарыда: жоғарыда солтүстік болуы үшін түртіңіз",
            compassPoints = listOf("С", "СШ", "Ш", "ОШ", "О", "ОБ", "Б", "СБ"),
        ),
        // Uzbek
        "uz" to OrnamentWords(
            zoomIn = "Kattalashtirish",
            zoomOut = "Kichiklashtirish",
            north = "Shimol",
            compass = "Kompas",
            credits = "Xarita manbalari",
            northUp = "Shimol tepada: harakat yoʻnalishi tepada boʻlishi uchun bosing",
            courseUp = "Harakat yoʻnalishi tepada: shimol tepada boʻlishi uchun bosing",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Arabic
        "ar" to OrnamentWords(
            zoomIn = "تكبير",
            zoomOut = "تصغير",
            north = "الشمال",
            compass = "البوصلة",
            credits = "مصادر الخريطة",
            northUp = "الشمال للأعلى: انقر لجعل اتجاه السير للأعلى",
            courseUp = "اتجاه السير للأعلى: انقر لجعل الشمال للأعلى",
            compassPoints = listOf("ش", "ش ق", "ق", "ج ق", "ج", "ج غ", "غ", "ش غ"),
        ),
        // Hebrew
        "he" to OrnamentWords(
            zoomIn = "התקרבות",
            zoomOut = "התרחקות",
            north = "צפון",
            compass = "מצפן",
            credits = "מקורות המפה",
            northUp = "צפון למעלה: הקישו כדי שכיוון הנסיעה יהיה למעלה",
            courseUp = "כיוון הנסיעה למעלה: הקישו כדי שהצפון יהיה למעלה",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Persian
        "fa" to OrnamentWords(
            zoomIn = "بزرگ‌نمایی",
            zoomOut = "کوچک‌نمایی",
            north = "شمال",
            compass = "قطب‌نما",
            credits = "منابع نقشه",
            northUp = "شمال بالا: برای بالا آمدن جهت حرکت ضربه بزنید",
            courseUp = "جهت حرکت بالا: برای بالا آمدن شمال ضربه بزنید",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Turkish
        "tr" to OrnamentWords(
            zoomIn = "Yakınlaştır",
            zoomOut = "Uzaklaştır",
            north = "Kuzey",
            compass = "Pusula",
            credits = "Harita kaynakları",
            northUp = "Kuzey yukarıda: hareket yönünü yukarı almak için dokunun",
            courseUp = "Hareket yönü yukarıda: kuzeyi yukarı almak için dokunun",
            compassPoints = listOf("K", "KD", "D", "GD", "G", "GB", "B", "KB"),
        ),
        // Azerbaijani
        "az" to OrnamentWords(
            zoomIn = "Böyüt",
            zoomOut = "Kiçilt",
            north = "Şimal",
            compass = "Kompas",
            credits = "Xəritə mənbələri",
            northUp = "Şimal yuxarıda: hərəkət istiqamətini yuxarı etmək üçün toxunun",
            courseUp = "Hərəkət istiqaməti yuxarıda: şimalı yuxarı etmək üçün toxunun",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Swahili
        "sw" to OrnamentWords(
            zoomIn = "Kuza",
            zoomOut = "Fifisha",
            north = "Kaskazini",
            compass = "Dira",
            credits = "Vyanzo vya ramani",
            northUp = "Kaskazini juu: gusa ili mwelekeo wa safari uwe juu",
            courseUp = "Mwelekeo wa safari juu: gusa ili kaskazini iwe juu",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Finnish
        "fi" to OrnamentWords(
            zoomIn = "Lähennä",
            zoomOut = "Loitonna",
            north = "Pohjoinen",
            compass = "Kompassi",
            credits = "Kartan lähteet",
            northUp = "Pohjoinen ylhäällä: napauta, niin kulkusuunta on ylhäällä",
            courseUp = "Kulkusuunta ylhäällä: napauta, niin pohjoinen on ylhäällä",
            compassPoints = listOf("P", "KO", "I", "KA", "E", "LO", "L", "LU"),
        ),
        // Estonian
        "et" to OrnamentWords(
            zoomIn = "Suurenda",
            zoomOut = "Vähenda",
            north = "Põhi",
            compass = "Kompass",
            credits = "Kaardi allikad",
            northUp = "Põhi üleval: puuduta, et üleval oleks liikumissuund",
            courseUp = "Liikumissuund üleval: puuduta, et üleval oleks põhi",
            compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        // Hungarian
        "hu" to OrnamentWords(
            zoomIn = "Nagyítás",
            zoomOut = "Kicsinyítés",
            north = "Észak",
            compass = "Iránytű",
            credits = "Térképforrások",
            northUp = "Észak felül: koppintson, hogy a haladási irány legyen felül",
            courseUp = "Haladási irány felül: koppintson, hogy észak legyen felül",
            compassPoints = listOf("É", "ÉK", "K", "DK", "D", "DNy", "Ny", "ÉNy"),
        ),
        // Greek
        "el" to OrnamentWords(
            zoomIn = "Μεγέθυνση",
            zoomOut = "Σμίκρυνση",
            north = "Βορράς",
            compass = "Πυξίδα",
            credits = "Πηγές χάρτη",
            northUp = "Βορράς επάνω: πατήστε για κατεύθυνση κίνησης επάνω",
            courseUp = "Κατεύθυνση κίνησης επάνω: πατήστε για βορρά επάνω",
            compassPoints = listOf("Β", "ΒΑ", "Α", "ΝΑ", "Ν", "ΝΔ", "Δ", "ΒΔ"),
        ),
        // Basque
        "eu" to OrnamentWords(
            zoomIn = "Handiagotu",
            zoomOut = "Txikiagotu",
            north = "Iparraldea",
            compass = "Iparrorratza",
            credits = "Maparen iturriak",
            northUp = "Iparraldea goian: ukitu bidaiaren norabidea goian jartzeko",
            courseUp = "Bidaiaren norabidea goian: ukitu iparraldea goian jartzeko",
            compassPoints = listOf("I", "IE", "E", "HE", "H", "HM", "M", "IM"),
        ),
        // Albanian
        "sq" to OrnamentWords(
            zoomIn = "Zmadho",
            zoomOut = "Zvogëlo",
            north = "Veri",
            compass = "Busull",
            credits = "Burimet e hartës",
            northUp = "Veriu lart: prekni që drejtimi i lëvizjes të jetë lart",
            courseUp = "Drejtimi i lëvizjes lart: prekni që veriu të jetë lart",
            compassPoints = listOf("V", "VL", "L", "JL", "J", "JP", "P", "VP"),
        ),
    )
}
