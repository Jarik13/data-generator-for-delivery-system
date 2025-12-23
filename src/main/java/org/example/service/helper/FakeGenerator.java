package org.example.service.helper;

import net.datafaker.Faker;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FakeGenerator {
    private static final Faker faker = new Faker(new Locale("uk"));
    private static final Random random = new Random();

    private static final String[] EMAIL_DOMAINS = { "gmail.com", "ukr.net", "i.ua", "yahoo.com", "outlook.com", "icloud.com" };

    public record PersonName(String firstName, String lastName, String middleName) {}

    public static PersonName generateName() {
        boolean isMale = random.nextBoolean();

        String firstName;
        String middleName;
        String lastName;

        if (isMale) {
            firstName = faker.expression("#{name.male_first_name}");
            middleName = faker.expression("#{name.male_middle_name}");
        } else {
            firstName = faker.expression("#{name.female_first_name}");
            middleName = faker.expression("#{name.female_middle_name}");
        }
        lastName = faker.name().lastName();

        return new PersonName(firstName, lastName, middleName);
    }

    public static String generateVariedPhone(int index, List<String> codes) {
        int codeIndex = index % codes.size();
        String code = codes.get(codeIndex);

        long localIndex = index / codes.size();
        long range = 9_000_000L;

        long scrambledIndex = (localIndex * 573293L) % range;
        long codeShift = (codeIndex * 817504L) % range;
        long finalBodyRaw = (scrambledIndex + codeShift) % range;

        long bodyValue = 1_000_000L + finalBodyRaw;

        String body = String.valueOf(bodyValue);

        int formatType = random.nextInt(3);

        return switch (formatType) {
            case 0 -> "+380" + code + body;
            case 1 -> "380" + code + body;
            default -> "0" + code + body;
        };
    }

    public static String generateEmail(String firstName, String lastName, int uniqueId) {
        String latFirstName = transliterate(firstName);
        String latLastName = transliterate(lastName);
        String domain = EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];
        return latFirstName + "." + latLastName + "." + uniqueId + "@" + domain;
    }

    public static String generateUniqueLicense(int index) {
        char c1 = (char) ('A' + (index % 26));
        char c2 = (char) ('A' + ((index / 26) % 26));
        char c3 = (char) ('A' + random.nextInt(26));
        long numVal = (index * 48611L) % 900000L + 100000L;
        return "" + c1 + c2 + c3 + numVal;
    }

    public static <T> T getRandomItem(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }

    private static String transliterate(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            switch (c) {
                case 'а': sb.append("a"); break;
                case 'б': sb.append("b"); break;
                case 'в': sb.append("v"); break;
                case 'г': sb.append("h"); break;
                case 'ґ': sb.append("g"); break;
                case 'д': sb.append("d"); break;
                case 'е': sb.append("e"); break;
                case 'є': sb.append("ye"); break;
                case 'ж': sb.append("zh"); break;
                case 'з': sb.append("z"); break;
                case 'и': sb.append("y"); break;
                case 'і': sb.append("i"); break;
                case 'ї': sb.append("yi"); break;
                case 'й': sb.append("y"); break;
                case 'к': sb.append("k"); break;
                case 'л': sb.append("l"); break;
                case 'м': sb.append("m"); break;
                case 'н': sb.append("n"); break;
                case 'о': sb.append("o"); break;
                case 'п': sb.append("p"); break;
                case 'р': sb.append("r"); break;
                case 'с': sb.append("s"); break;
                case 'т': sb.append("t"); break;
                case 'у': sb.append("u"); break;
                case 'ф': sb.append("f"); break;
                case 'х': sb.append("kh"); break;
                case 'ц': sb.append("ts"); break;
                case 'ч': sb.append("ch"); break;
                case 'ш': sb.append("sh"); break;
                case 'щ': sb.append("shch"); break;
                case 'ь', '\'': break;
                case 'ю': sb.append("yu"); break;
                case 'я': sb.append("ya"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}