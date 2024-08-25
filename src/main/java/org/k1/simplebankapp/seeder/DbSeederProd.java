package org.k1.simplebankapp.seeder;

import org.k1.simplebankapp.config.Config;
import org.k1.simplebankapp.entity.*;
import org.k1.simplebankapp.entity.enums.AccountType;
import org.k1.simplebankapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Profile("prod")
public class DbSeederProd implements ApplicationRunner {

    private final Logger logger = LoggerFactory.getLogger(DbSeederProd.class);

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolePathRepository rolePathRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankRepository bankRepository;

    private final String[] users = {
            "admin@mail.com:ROLE_SUPERUSER ROLE_USER ROLE_ADMIN:ADMIN",
            "diana:ROLE_USER:Septriediana Amalia",
            "azizah:ROLE_USER:Noer Azizah Permata Sonda",
            "nur:ROLE_USER:Muhammad Nur Setiawan",
            "azis:ROLE_USER:Nur Azis Kurnia Rianto",
            "azisz:ROLE_USER:Ahmad Miftahul Azisz",
            "ana:ROLE_USER:Ana Bellatus Mustaqfiro",
            "ridho:ROLE_USER:Ridho Gymnastiar Al Rasyid",
            "aulia:ROLE_USER:Aulia Kuswina Gusta",
            "diams:ROLE_USER:Dimas Aditya Purwadi Putra",
            "yulinar:ROLE_USER:Yulinar",
            "ahmad:ROLE_USER:Ahmad kevin",
            "adnika:ROLE_USER:Andika Tirta",
            "aji:ROLE_USER:Aji Nuansa",
            "sahid:ROLE_USER:Sahid Jafar",
            "fauzan:ROLE_USER:Fauzan Nursalma",
            "jeremia:ROLE_USER:Jeremia Letare Pane",
            "bagus:ROLE_USER:Bagus Angkasawan Sumantri Putra",
            "andi:ROLE_USER:Andi Eka Nugraha",
            "hasan:ROLE_USER:M. Hasan",
            "yohanna:ROLE_USER:Yohanna Melani Sihotang",
            "dandy:ROLE_USER:Dandy Dicky Triwibowo",
            "qa.api:ROLE_USER:QA API TESTING PROD",
            "qa.mobile:ROLE_USER:QA MOBILE TESTING PROD",
            "qa.website:ROLE_USER:QA WEBSITE TESTING PROD"
    };

    private final String[] roles = {
            "ROLE_SUPERUSER:user_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS",
            "ROLE_ADMIN:user_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS",
            "ROLE_USER:user_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS",
            "ROLE_READ:oauth_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS",
            "ROLE_WRITE:oauth_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS"
    };

    private final String[] clients = {
            "my-client-apps:ROLE_READ ROLE_WRITE",
            "my-client-web:ROLE_READ ROLE_WRITE"
    };

    private final String[] banks = {
            "BCA",
            "BRI",
            "Mandiri",
            "BNI",
            "Permata Bank",
            "Superbank",
            "BTPN"
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        logger.info("Starting database seeder for production...");

        if (isProductionSafeToSeed()) {
            String defaultPassword = "$1mpl3b4nk@2024";
            String password = encoder.encode(defaultPassword);

            insertBanks();
            insertRoles();
            insertClients(password);
            insertUsers(password);
            insertAccounts();

            logger.info("Database seeder for production completed successfully.");
        } else {
            logger.warn("Database seeding is not safe to run in production. Aborting.");
        }
    }

    @Transactional
    public void insertRoles() {
        for (String role : roles) {
            String[] str = role.split(":");
            String name = str[0];
            String type = str[1];
            String pattern = str[2];
            String[] methods = str[3].split("\\|");
            Role oldRole = roleRepository.findOneByName(name);
            if (oldRole == null) {
                oldRole = new Role();
                oldRole.setName(name);
                oldRole.setType(type);
                oldRole.setRolePaths(new ArrayList<>());
                for (String m : methods) {
                    String rolePathName = name.toLowerCase() + "_" + m.toLowerCase();
                    RolePath rolePath = rolePathRepository.findOneByName(rolePathName);
                    if (rolePath == null) {
                        rolePath = new RolePath();
                        rolePath.setName(rolePathName);
                        rolePath.setMethod(m.toUpperCase());
                        rolePath.setPattern(pattern);
                        rolePath.setRole(oldRole);
                        rolePathRepository.save(rolePath);
                        oldRole.getRolePaths().add(rolePath);
                    }
                }
                roleRepository.save(oldRole);
            }
        }
    }

    @Transactional
    public void insertClients(String password) {
        for (String c : clients) {
            String[] s = c.split(":");
            String clientName = s[0];
            String[] clientRoles = s[1].split("\\s");
            Client oldClient = clientRepository.findOneByClientId(clientName);
            if (oldClient == null) {
                oldClient = getClient(password, clientName, clientRoles, roleRepository);
                clientRepository.save(oldClient);
            }
        }
    }

    static Client getClient(String password, String clientName, String[] clientRoles, RoleRepository roleRepository) {
        Client oldClient = new Client();
        oldClient.setClientId(clientName);
        oldClient.setAccessTokenValiditySeconds(28800); // 8 hours
        oldClient.setRefreshTokenValiditySeconds(7257600); // 84 days
        oldClient.setGrantTypes("password refresh_token authorization_code");
        oldClient.setClientSecret(password);
        oldClient.setApproved(true);
        oldClient.setRedirectUris("");
        oldClient.setScopes("read write");
        List<Role> rls = roleRepository.findByNameIn(clientRoles);
        if (!rls.isEmpty()) {
            oldClient.getAuthorities().addAll(rls);
        }
        return oldClient;
    }

    @Transactional
    public void insertUsers(String password) {
        for (String userNames : users) {
            String[] str = userNames.split(":");
            String username = str[0];
            String[] roleNames = str[1].split("\\s");
            String fullName = str[2];
            User oldUser = userRepository.findByUsername(username);
            if (oldUser == null) {
                oldUser = new User();
                oldUser.setUsername(username);
                oldUser.setPassword(password);
                oldUser.setFullname(fullName);
                oldUser.setBornDate(LocalDateTime.of(2000, Month.NOVEMBER, 11, 1, 1, 0));
                oldUser.setEmail(username + "@mail.com");
                oldUser.setPhoneNumber("0812345678" + Config.randomString(2, true));
                oldUser.setLoginAttempts(0);
                List<Role> r = roleRepository.findByNameIn(roleNames);
                oldUser.setRoles(r);
                userRepository.save(oldUser);
            }
        }
    }

    @Transactional
    public void insertAccounts() {
        if (accountRepository.count() == 0) {
            LocalDate localDate = LocalDate.of(2025, 11, 22);
            Date customDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            for (String username : users) {
                Account account = new Account();
                account.setNo("317103001" + Config.randomString(8, true));
                account.setAccountType(AccountType.GOLD);
                account.setCardNumber("373101001" + Config.randomString(8, true));
                account.setExpDate(customDate);
                account.setBalance(1000000000L);
                account.setBank(bankRepository.findById(1L).orElse(null));
                account.setPin("123456");
                account.setPinAttempts(0);
                User user = userRepository.findByUsername(username);
                if (user != null) {
                    account.setUser(user);
                    accountRepository.save(account);
                }
            }
        }
    }

    @Transactional
    public void insertBanks() {
        if (bankRepository.count() == 0) {
            for (String bank : banks) {
                Bank newBank = new Bank();
                newBank.setBankName(bank);
                newBank.setAdminFee(bank.equals("BCA") ? 0 : 2500);
                bankRepository.save(newBank);
            }
        }
    }

    private boolean isProductionSafeToSeed() {
        // Implement any necessary checks to ensure it is safe to seed the database in production.
        // This might include checking for an environment variable, a specific command-line argument,
        // or some other safety mechanism.
        return false; // Change this to your actual safety check
    }
}

