package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

 @Autowired
  private AccountsService accountsService;

  @MockBean
  private AccountsRepository accountsRepository;

  @MockBean
  private NotificationService notificationService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  public void transferMoney_shouldNotifyBothAccounts() {
    // Arrange
    Account accountFrom = new Account("123");
    Account accountTo = new Account("456");
    accountFrom.setBalance(new BigDecimal("1000"));
    accountTo.setBalance(new BigDecimal("500"));

    BigDecimal transferAmount = new BigDecimal("200");

    when(accountsRepository.getAccount("123")).thenReturn(accountFrom);
    when(accountsRepository.getAccount("456")).thenReturn(accountTo);


    accountsService.transferMoney("123", "456", transferAmount);
    verify(notificationService).notifyAboutTransfer(accountFrom, "Transferred 200 to account456");
    verify(notificationService).notifyAboutTransfer(accountTo, "Received 200 from account123");

    verifyNoMoreInteractions(notificationService);
  }

  @Test
  public void transferMoney_shouldThrowInsufficientFundsException() {

    Account accountFrom = new Account("123");
    Account accountTo = new Account("456");
    accountFrom.setBalance(new BigDecimal("100"));
    accountTo.setBalance(new BigDecimal("500"));

    BigDecimal transferAmount = new BigDecimal("200");

    when(accountsRepository.getAccount("123")).thenReturn(accountFrom);
    when(accountsRepository.getAccount("456")).thenReturn(accountTo);

    assertThrows(InsufficientFundException.class, () -> accountsService.transferMoney("123", "456", transferAmount));

    verify(notificationService, never()).notifyAboutTransfer(any(), any());
  }

  @Test
  public void transferMoney_shouldThrowIllegalArgumentException_forNegativeAmount() {

    assertThrows(IllegalArgumentException.class, () -> accountsService.transferMoney("123", "456", new BigDecimal("-100")));
  }
}
