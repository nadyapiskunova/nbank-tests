package ui.pages;

import api.models.TransactionType;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import ui.elements.TransactionItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TransferPage extends BasePage<TransferPage> {
    private SelenideElement
            inputUsername =
                    $(Selectors.byAttribute("placeholder", "Enter recipient name")),
            inputReceiverAccountNumber =
                    $(Selectors.byAttribute("placeholder", "Enter recipient account number")),
            confirmCheck = $("#confirmCheck"),
            transferBtn = $(".btn-primary.shadow-custom.green-btn.mt-4"),
            transferAgainBtn = $(".custom-btn.shadow-custom.gray-btn"),
            nameSearchInput = $(".form-control"),
            searchTransactionBtn = $(".custom-btn.shadow-custom.blue-btn.mt-3"),
            foundUnderLabel = $(".list-group-item small"),
            repeatBtn = $(".custom-btn.shadow-custom.pink-btn"),
            accountIdLabel = $("p strong"),
            accountList  = $("select.form-control"),
            sendTransferBtn = $(".btn.btn-success");


    private ElementsCollection transactionItems = $$("li.list-group-item");

    @Override
    public String url() {
        return "/transfer";
    }

    public List<TransactionItem> getTransactions() {
        return generatePageElement(
                transactionItems,
                TransactionItem::new
        );
    }

    public TransferPage selectAccount(String senderAccountNumber){
        accountsSelector.selectOptionContainingText(senderAccountNumber);

        return this;
    }

    public TransferPage setUsername(String username){
        inputUsername.setValue(username);

        return this;
    }

    public TransferPage setReceiverAccountNumber(String receiverAccountNumber){
        inputReceiverAccountNumber.setValue(receiverAccountNumber);
        return this;
    }

    public TransferPage confirmCheckbox() {
        confirmCheck.setSelected(true);
        return this;
    }

    public TransferPage clickTransferButton(){
        transferBtn.click();

        return this;
    }

    public TransferPage checkTransactionIsDisplayed(TransactionType transactionType, double amount) {
        double expectedAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
        boolean transactionIsDisplayed = getTransactions().stream()
                        .anyMatch(transaction ->
                                transaction.getTransactionType() == transactionType && Double.compare(

                                        transaction.getAmount(),

                                        expectedAmount

                                ) == 0);

        assertThat(transactionIsDisplayed).isTrue();
        return this;
    }

    public TransferPage checkTransferTransactionsAreNotDisplayed() {
        transactionItems.filterBy(text(TransactionType.TRANSFER_IN.name()))
                .shouldHave(size(0));

        transactionItems.filterBy(text(TransactionType.TRANSFER_OUT.name()))
                .shouldHave(size(0));

        return this;
    }

    public TransferPage clickTransferAgainButton(){
        transferAgainBtn.click();

        return this;
    }
    public TransferPage searchByName(String name){
        nameSearchInput.sendKeys(name);

        return this;
    }

    public TransferPage clickSearchTransactionButton(){
        searchTransactionBtn.click();

        return this;
    }

    public TransferPage checkFoundUnder(String name) {
        foundUnderLabel.shouldHave(text(UiText.FOUND_UNDER.getText() + name));
        return this;
    }

    public TransferPage repeatTransaction(TransactionType transactionType) {
        getTransactions().stream()
                .filter(transaction ->
                        transaction.getTransactionType() == transactionType)
                .findFirst()
                .orElseThrow()
                .getRepeatButton()
                .click();
        return this;
    }

    public TransferPage checkAccountId(Integer accountId) {
        accountIdLabel.shouldHave(exactText(String.valueOf(accountId)));

        return this;
    }

    public TransferPage selectSenderAccountNumber(String accountNumber){
        accountList.selectOptionContainingText(accountNumber);

        return this;
    }

    public TransferPage clickSendTransferButton(){
        sendTransferBtn.click();

        return this;
    }

}
