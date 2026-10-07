package ua.moki.modules.products.enums;

public enum ProductCategory {
    DRIED_FRUITS("Сухофрукти", "4628537", "23403"),
    NUTS("Горіхи", "4645744", "104"),
    SWEETS("Солодощі", "4633017", "24001"),
    CANDIES("Цукерки", "4629506", "21803"),
    SUPER_FOOD("Суперфуди", "4629655", "217"),
    OIL_AND_BUTTERS("Олії та масла", "4627757", "21302"),
    CONSERVATION("Консервація", "4633041", "21406"),
    TEA("Чай", "4625004", "10701"),
    COFFEE("Кава", "4625011", "10702"),
    SNACKS_AND_CHIPS("Снеки та чіпси", "4627680", "215"),
    SPICES("Спеції", "4645648", "212");

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
