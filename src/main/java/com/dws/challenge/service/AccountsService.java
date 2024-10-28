package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientFundException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
public class AccountsService {

@Getter
  private final AccountsRepository accountsRepository;

  private final NotificationService notificationService;

  @Autowired
  public AccountsService( AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService =notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public  synchronized void transferMoney(String accountFromId, String accountToId, BigDecimal amount) {
    if(amount.compareTo(BigDecimal.ZERO)<=0){
      throw new IllegalArgumentException("TransferAmount should be positive");
    }
    Account accountFrom = accountsRepository.getAccount(accountFromId);
    Account accountTo = accountsRepository.getAccount(accountToId);
    synchronized (accountFrom){
      synchronized (accountTo){
        if(accountFrom.getBalance().compareTo(amount)<0){
          throw new InsufficientFundException("Insufficient Funds in account"+accountFromId);
        }

        accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
        accountTo.setBalance(accountTo.getBalance().add(amount));
        notificationService.notifyAboutTransfer(accountFrom,"Transferred " + amount + " to account"+ accountToId);
        notificationService.notifyAboutTransfer(accountTo,"Received " + amount + " from account"+ accountFromId);
      }
    }
  }

}
