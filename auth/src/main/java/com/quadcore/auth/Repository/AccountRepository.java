package com.quadcore.auth.Repository;

import com.quadcore.auth.Domain.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Integer> {
    Account findByUsername(String username);
    Account findByEmail(String email);
    Long deleteByUsername(String username);
}
