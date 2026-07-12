package ui.elements;

import api.models.TransactionType;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

@Getter
public class TransactionItem extends BaseElement {
    private TransactionType transactionType;
    private double amount;
    private String username;

    private SelenideElement repeatButton;
    public TransactionItem(SelenideElement element) {
        super(element);
        String transactionText = element.$("span").getText().split("\n")[0];
        String[] transactionData = transactionText.split(" - \\$");
        transactionType = TransactionType.valueOf(transactionData[0]);
        amount = Double.parseDouble(transactionData[1]);
        username = element.$("strong").getText();
        repeatButton = element.$("button");
    }


}
