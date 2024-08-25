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
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Profile({"dev", "stg"})
public class DbSeederDevStg implements ApplicationRunner {

    private Logger logger = LoggerFactory.getLogger(DbSeederDevStg.class);

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

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private QrisPaymentRepository qrisPaymentRepository;

    private String defaultPassword = "password";

    private String[] users = new String[]{
            "admin@mail.com:ROLE_SUPERUSER ROLE_USER ROLE_ADMIN",
            "userbank1:ROLE_USER",
            "userbank2:ROLE_USER",
            "userbank3:ROLE_USER",
            "userbank4:ROLE_USER",
            "userbank5:ROLE_USER",
            "userbank6:ROLE_USER",
            "userbank7:ROLE_USER",
            "userbank8:ROLE_USER",
            "qa.api:ROLE_USER",
            "qa.mobile:ROLE_USER",
            "qa.website:ROLE_USER",
    };

    private String[] clients = new String[]{
            "my-client-apps:ROLE_READ ROLE_WRITE", // mobile
            "my-client-web:ROLE_READ ROLE_WRITE" // web
    };

    private String[] roles = new String[]{
            "ROLE_SUPERUSER:user_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS",
            "ROLE_ADMIN:user_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS",
            "ROLE_USER:user_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS",
            "ROLE_READ:oauth_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS",
            "ROLE_WRITE:oauth_role:^/.*:GET|PUT|POST|PATCH|DELETE|OPTIONS"
    };

    private String[] banks = new String[]{
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
    public void run(ApplicationArguments applicationArguments) {
        String password = encoder.encode(defaultPassword);

        this.insertBanks();
        this.insertRoles();
        this.insertClients(password);
//        this.insertUser(password);
//        this.insertAccount(password);
        this.insertMerchants();
        this.insertDummyMerchantsQRIS();
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
            if (null == oldRole) {
                oldRole = new Role();
                oldRole.setName(name);
                oldRole.setType(type);
                oldRole.setRolePaths(new ArrayList<>());
                for (String m : methods) {
                    String rolePathName = name.toLowerCase() + "_" + m.toLowerCase();
                    RolePath rolePath = rolePathRepository.findOneByName(rolePathName);
                    if (null == rolePath) {
                        rolePath = new RolePath();
                        rolePath.setName(rolePathName);
                        rolePath.setMethod(m.toUpperCase());
                        rolePath.setPattern(pattern);
                        rolePath.setRole(oldRole);
                        rolePathRepository.save(rolePath);
                        oldRole.getRolePaths().add(rolePath);
                    }
                }
            }

            roleRepository.save(oldRole);
        }
    }

    @Transactional
    public void insertClients(String password) {
        for (String c : clients) {
            String[] s = c.split(":");
            String clientName = s[0];
            String[] clientRoles = s[1].split("\\s");
            Client oldClient = clientRepository.findOneByClientId(clientName);
            if (null == oldClient) {
                oldClient = new Client();
                oldClient.setClientId(clientName);
                oldClient.setAccessTokenValiditySeconds(28800);//1 jam 3600 :token valid : seharian kerja : normal 1 jam
                oldClient.setRefreshTokenValiditySeconds(7257600);// refresh
                oldClient.setGrantTypes("password refresh_token authorization_code");
                oldClient.setClientSecret(password);
                oldClient.setApproved(true);
                oldClient.setRedirectUris("");
                oldClient.setScopes("read write");
                List<Role> rls = roleRepository.findByNameIn(clientRoles);

                if (!rls.isEmpty()) {
                    oldClient.getAuthorities().addAll(rls);
                }
            }
            clientRepository.save(oldClient);
        }
    }

    @Transactional
    public void insertUser(String password) {
        int counter = 0;
        for (String userNames : users) {
            String[] str = userNames.split(":");
            String username = str[0];
            String[] roleNames = str[1].split("\\s");
            String fullName = "User Bank";
            User oldUser = userRepository.findByUsername(username);
            if (null == oldUser) {
                oldUser = new User();
                oldUser.setUsername(username);
                oldUser.setPassword(password);
                oldUser.setFullname(fullName);
                oldUser.setBornDate(LocalDateTime.of(2002, Month.NOVEMBER, 22, 10, 30, 0));
                oldUser.setEmail(username + "@mail.com");
                oldUser.setPhoneNumber("08123456789" + counter);
                oldUser.setLoginAttempts(0);
                oldUser.setAddress("TEST");
                oldUser.setNik("TEST");
                List<Role> r = roleRepository.findByNameIn(roleNames);
                oldUser.setRoles(r);
            }

            userRepository.save(oldUser);
            counter++;
        }
    }

    @Transactional
    public void insertAccount(String password) {
        if (accountRepository.count() == 0) {
            int counter = 0;
            LocalDate localDate = LocalDate.of(2025, 11, 22);
            Date customDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            for (String userNames : users) {
                Account account = new Account();
                account.setNo("373765759821356" + counter);
                account.setAccountType(AccountType.GOLD);
                account.setCardNumber("373765759821351" + counter);
                account.setExpDate(customDate);
                account.setBalance(1000000000L);
                account.setBank(bankRepository.findById(1L).get());
                account.setPin("123456");
                account.setPinAttempts(0);
                String[] str = userNames.split(":");
                String username = str[0];
                String[] roleNames = str[1].split("\\s");
                User user = userRepository.findByUsername(username);
                if (null == user) {
                    user = new User();
                    user.setUsername(username);
                    user.setPassword(password);
                    List<Role> r = roleRepository.findByNameIn(roleNames);
                    user.setRoles(r);
                }
                account.setUser(user);
                accountRepository.save(account);
                counter++;
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

    @Transactional
    public void insertMerchants() {
        if (merchantRepository.count() == 0) {
            for (int i = 0; i < 10; i++) {
                Merchant newMerchant = new Merchant();
                newMerchant.setName("Toko Madura " + (i + 1));
                newMerchant.setBalance(0.0);
                merchantRepository.save(newMerchant);
            }
        }
    }

    @Transactional
    public void insertDummyMerchantsQRIS() {
        if (qrisPaymentRepository.count() == 0) {
            merchantRepository.findAll().forEach(merchant -> {
                QrisPayment qrisPayment = new QrisPayment();
                qrisPayment.setQrisCode("MRCHNT" + Config.randomString(10, true));
                qrisPayment.setAccountNo(merchant.getId().toString());
                qrisPaymentRepository.save(qrisPayment);
            });
        }
    }
}
