package fr.soft_labs.eventbyte.domain.event;

final class Text {

    private Text() {
    }

    static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " est obligatoire");
        }
        return value;
    }
}
