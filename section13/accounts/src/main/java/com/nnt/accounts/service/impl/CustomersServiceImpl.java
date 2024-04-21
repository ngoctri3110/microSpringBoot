package com.nnt.accounts.service.impl;

import com.nnt.accounts.dto.AccountsDto;
import com.nnt.accounts.dto.CardsDto;
import com.nnt.accounts.dto.CustomerDetailsDto;
import com.nnt.accounts.dto.LoansDto;
import com.nnt.accounts.entity.Accounts;
import com.nnt.accounts.entity.Customer;
import com.nnt.accounts.exception.ResourceNotFoundException;
import com.nnt.accounts.mapper.AccountsMapper;
import com.nnt.accounts.mapper.CustomerMapper;
import com.nnt.accounts.repository.AccountsRepository;
import com.nnt.accounts.repository.CustomerRepository;
import com.nnt.accounts.service.ICustomersService;
import lombok.AllArgsConstructor;
import com.nnt.accounts.service.client.CardsFeignClient;
import com.nnt.accounts.service.client.LoansFeignClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomersServiceImpl implements ICustomersService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    @Qualifier("cardsFallback")
    private CardsFeignClient cardsFeignClient;
    @Qualifier("loansFallback")
    private LoansFeignClient loansFeignClient;

    /**
     * @param mobileNumber - Input Mobile Number
     * @param correlationId - Correlation ID value generated at Edge server
     * @return Customer Details based on a given mobileNumber
     */
    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        ResponseEntity<LoansDto> loansDtoResponseEntity = loansFeignClient.fetchLoanDetails(correlationId, mobileNumber);
        if(null != loansDtoResponseEntity) {
            customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());
        }

        ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsFeignClient.fetchCardDetails(correlationId, mobileNumber);
        if(null != cardsDtoResponseEntity) {
            customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());
        }

        return customerDetailsDto;
    }
}
