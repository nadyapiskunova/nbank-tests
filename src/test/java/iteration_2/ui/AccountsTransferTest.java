package iteration_2.ui;

import api.models.*;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import api.constans.TestConstants;
import api.generators.RandomData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

public class AccountsTransferTest extends BaseTest {
    @Test
    public void userCanTransferWithValidDataTest(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        AccountResponse secondAccount = UserSteps.createAccount(user);
                UserSteps.deposit(
                        user,
                        firstAccount.getId(),
                        TestConstants.MAX_DEPOSIT_AMOUNT
                );
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK(), Endpoint.LOGIN)
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/transfer");

        $(".account-selector").selectOptionContainingText(firstAccount.getAccountNumber());

        $(Selectors.byAttribute("placeholder","Enter recipient name"))
                .setValue(user.getUsername());

        $(Selectors.byAttribute("placeholder","Enter recipient account number"))
                .setValue(secondAccount.getAccountNumber());

        Double amount = RandomData.getTransferAmount();
        $(Selectors.byAttribute("placeholder","Enter amount"))
                .setValue(String.valueOf(amount));
        $("#confirmCheck").setSelected(true);
        $(".btn-primary.shadow-custom.green-btn.mt-4").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("✅ Successfully transferred ");
        alert.accept();

        List<AccountResponse> accountsAfterTransfer = UserSteps.getAccounts(user);

        AccountResponse senderAccount = accountsAfterTransfer.stream()
                .filter(account -> account.getId().equals(firstAccount.getId()))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterTransfer.stream()
                .filter(account -> account.getId().equals(secondAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance())
                .isCloseTo(TestConstants.MAX_DEPOSIT_AMOUNT - amount, within(0.001));

        softly.assertThat(receiverAccount.getBalance())
                .isCloseTo(amount, within(0.001));

        softly.assertThat(senderAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_OUT);

        softly.assertThat(receiverAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_IN);

    }

    @Test
    public void userCannotTransferWithInvalidDataTest() {
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        AccountResponse secondAccount = UserSteps.createAccount(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK(), Endpoint.LOGIN)
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/transfer");

        $(".account-selector").selectOptionContainingText(firstAccount.getAccountNumber());

        $(Selectors.byAttribute("placeholder", "Enter recipient name"))
                .setValue(user.getUsername());

        $(Selectors.byAttribute("placeholder", "Enter recipient account number"))
                .setValue(secondAccount.getAccountNumber());

        Double amount = TestConstants.ZERO_AMOUNT;
        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .setValue(String.valueOf(amount));
        $("#confirmCheck").setSelected(true);
        $(".btn-primary.shadow-custom.green-btn.mt-4").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Error: Transfer amount must be at least 0.01");
        alert.accept();
    }

    @Test
    public void userCannotTransferWithoutConfirmTest() {
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        AccountResponse secondAccount = UserSteps.createAccount(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK(), Endpoint.LOGIN)
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/transfer");

        $(".account-selector").selectOptionContainingText(firstAccount.getAccountNumber());

        $(Selectors.byAttribute("placeholder", "Enter recipient name"))
                .setValue(user.getUsername());

        $(Selectors.byAttribute("placeholder", "Enter recipient account number"))
                .setValue(secondAccount.getAccountNumber());

        Double amount = RandomData.getTransferAmount();
        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .setValue(String.valueOf(amount));
        $("#confirmCheck").setSelected(true);
        $(".btn-primary.shadow-custom.green-btn.mt-4").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Please fill all fields and confirm.");
        alert.accept();
    }

    @Test
    public void userCanSearchTransactionWithValidName(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        UpdateProfileRequest updateName = UserSteps.updateName(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK(), Endpoint.LOGIN)
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        Selenide.open("/transfer");
        $(".custom-btn.shadow-custom.gray-btn").click();
        $(".form-control").sendKeys(updateName.getName());
        $(".custom-btn.shadow-custom.blue-btn.mt-3").click();
        $(".list-group-item small").shouldHave(text("Found under: " + updateName.getName()));
    }

    @Test
    public void userCannotSearchTransactionWithInvalidName(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        UserSteps.updateName(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK(), Endpoint.LOGIN)
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/transfer");

        $(".custom-btn.shadow-custom.gray-btn").click();
        $(".form-control").sendKeys(RandomData.getNameWithoutSurname());
        $(".custom-btn.shadow-custom.blue-btn.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ No matching users found.");
        alert.accept();
    }

    @Disabled("Баг: в popup повтора операции TRANSFER_IN отображается firstAccount.getId()")
    @Test
    public void userCanRepeatTransfer(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        AccountResponse secondAccount = UserSteps.createAccount(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );
        double amountTransfer = RandomData.getTransferAmount();
        UserSteps.transfer(
                user,
                firstAccount.getId(),
                secondAccount.getId(),
                amountTransfer);

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK(), Endpoint.LOGIN)
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        Selenide.open("/transfer");
        $(".custom-btn.shadow-custom.gray-btn").click();
        $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"))
                .$(".custom-btn")
                .click();
        $("p strong").shouldHave(exactText(String.valueOf(secondAccount.getId())));
        $("select.form-control")
                .selectOptionContainingText(firstAccount.getAccountNumber());
        $("#confirmCheck").setSelected(true);
        $(".btn.btn-success").click();
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("✅ Transfer of");
        alert.accept();

        double expectedSenderBalance =
                TestConstants.MAX_DEPOSIT_AMOUNT - amountTransfer - amountTransfer;

        double expectedReceiverBalance = amountTransfer + amountTransfer;

        List<AccountResponse> accountsAfterRepeatTransfer = UserSteps.getAccounts(user);

        AccountResponse senderAccount = accountsAfterRepeatTransfer.stream()
                .filter(account -> account.getId().equals(firstAccount.getId()))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterRepeatTransfer.stream()
                .filter(account -> account.getId().equals(secondAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance()).isCloseTo(expectedSenderBalance, within(0.001));
        softly.assertThat(receiverAccount.getBalance()).isCloseTo(expectedReceiverBalance, within(0.001));

    }
}
