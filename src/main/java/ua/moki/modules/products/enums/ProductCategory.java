package ua.moki.modules.products.enums;

public enum ProductCategory {
    DRIED_FRUITS("Сухофрукти", "111111", "22222"), // (Назва, rozetkaId, promId)
    NUTS("Горіхи", "333333", "44444"),
    SWEETS("Солодощі", "555555", "66666"),
    CANDIES("Цукерки", "4629506", "12345"),
    SUPER_FOOD("Суперфуди", "777777", "88888"),
    OIL_AND_BUTTERS("Олії та масла", "999999", "00000"),
    CONSERVATION("Консервація", "121212", "13131"),
    TEA("Чай", "141414", "15151"),
    COFFEE("Кава", "161616", "17171"),
    SNACKS_AND_CHIPS("Снеки та чіпси", "181818", "19191"),
    SPICES("Спеції", "202020", "21212");

    private final String title;
    private final String rozetkaId;
    private final String promId;

    ProductCategory(String title, String rozetkaId, String promId) {
        this.title = title;
        this.rozetkaId = rozetkaId;
        this.promId = promId;
    }

    public String getTitle() { return title; }
    public String getRozetkaId() { return rozetkaId; }
    public String getPromId() { return promId; }
}
