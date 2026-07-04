package ui.pages;

import lombok.Getter;

@Getter
public enum UiText {
    FOUND_UNDER("Found under: ");

    private final String text;

    UiText(String text) {
        this.text = text;
    }
}
