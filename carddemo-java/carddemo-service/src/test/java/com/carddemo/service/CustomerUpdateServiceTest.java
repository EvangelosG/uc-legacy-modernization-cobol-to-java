package com.carddemo.service;

import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerUpdateServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerUpdateService customerUpdateService;

    @BeforeEach
    void setUp() {
        ValidationService validationService = new ValidationService();
        customerUpdateService = new CustomerUpdateService(customerRepository, validationService);
    }

    private Customer sampleCustomer() {
        return Customer.builder()
                .custId(100L).firstName("JOHN").middleName("Q").lastName("DOE")
                .addrLine1("123 MAIN ST").addrStateCd("NY").addrCountryCd("US")
                .addrZip("10001").phoneNum1("2125551234").ssn("123456789")
                .dob(LocalDate.of(1990, 1, 15)).ficoCreditScore(750)
                .build();
    }

    @Test
    void updateCustomerSuccessfully() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerUpdateService.updateCustomer(100L,
                new CustomerUpdateService.CustomerUpdateCommand(
                        "JANE", null, "SMITH", null, null, null,
                        null, null, null, null, null, null, null, null));

        assertThat(result.getFirstName()).isEqualTo("JANE");
        assertThat(result.getLastName()).isEqualTo("SMITH");
    }

    @Test
    void updateCustomerThrowsWhenNotFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerUpdateService.updateCustomer(999L,
                new CustomerUpdateService.CustomerUpdateCommand(
                        null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null)))
                .isInstanceOf(TransactionTypeService.ResourceNotFoundException.class);
    }

    @Test
    void updateCustomerRejectsInvalidSSN() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerUpdateService.updateCustomer(100L,
                new CustomerUpdateService.CustomerUpdateCommand(
                        null, null, null, null, null, null,
                        null, null, null, null, null, "000456789", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SSN area code");
    }

    @Test
    void updateCustomerRejectsInvalidStateCode() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerUpdateService.updateCustomer(100L,
                new CustomerUpdateService.CustomerUpdateCommand(
                        null, null, null, null, null, null,
                        "ZZ", null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid US state code");
    }

    @Test
    void updateCustomerRejectsFutureDob() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerUpdateService.updateCustomer(100L,
                new CustomerUpdateService.CustomerUpdateCommand(
                        null, null, null, null, null, null,
                        null, null, null, null, null, null,
                        LocalDate.of(2099, 1, 1), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date of birth cannot be in the future");
    }

    @Test
    void updateCustomerRejectsBlankFirstName() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerUpdateService.updateCustomer(100L,
                new CustomerUpdateService.CustomerUpdateCommand(
                        "  ", null, null, null, null, null,
                        null, null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First name must not be blank");
    }

    @Test
    void updateCustomerAcceptsValidSSN() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerUpdateService.updateCustomer(100L,
                new CustomerUpdateService.CustomerUpdateCommand(
                        null, null, null, null, null, null,
                        null, null, null, null, null, "123456789", null, null));

        assertThat(result.getSsn()).isEqualTo("123456789");
    }

    @Test
    void updateCustomerAcceptsValidPhoneNumber() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerUpdateService.updateCustomer(100L,
                new CustomerUpdateService.CustomerUpdateCommand(
                        null, null, null, null, null, null,
                        null, null, null, "(212)555-1234", null, null, null, null));

        assertThat(result.getPhoneNum1()).isEqualTo("(212)555-1234");
    }
}
