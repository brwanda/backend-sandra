package com.earacg.earaconnect.config;

import com.earacg.earaconnect.model.Eac;
import com.earacg.earaconnect.model.Committee;
import com.earacg.earaconnect.model.Country;
import com.earacg.earaconnect.model.RevenueAuthority;
import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.model.CountryCommitte;
import com.earacg.earaconnect.model.CountrySubCommittee;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.CommitteeRepo;
import com.earacg.earaconnect.repository.EacRepo;
import com.earacg.earaconnect.repository.CountryRepo;
import com.earacg.earaconnect.repository.RevenueAuthorityRepo;
import com.earacg.earaconnect.repository.SubCommitteeRepo;
import com.earacg.earaconnect.repository.CountryCommiteRepo;
import com.earacg.earaconnect.repository.CountrySubCommitteeRepo;
import com.earacg.earaconnect.repository.UserRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private EacRepo eacRepo;
    
    @Autowired
    private CountryRepo countryRepo;
    
    @Autowired
    private RevenueAuthorityRepo revenueAuthorityRepo;

    @Autowired
    private CommitteeRepo committeeRepo;

    @Autowired
    private SubCommitteeRepo subCommitteeRepo;

    @Autowired
    private CountryCommiteRepo countryCommitteeRepo;

    @Autowired
    private CountrySubCommitteeRepo countrySubCommitteeRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeEacData();
        initializeCountryData();
        initializeRevenueAuthorityData();
        initializeCommitteeData();
        initializeSubCommitteeData();
        initializeCountryCommitteeMapping();
        initializeCountrySubCommitteeMapping();
        initializeDefaultAdminUser();
        System.out.println("Data initialization completed successfully");
    }

    private void initializeEacData() {
        if (eacRepo.count() == 0) {
            Eac eac = new Eac();
            eac.setName("East African Community");
            eacRepo.save(eac);
            System.out.println("EAC data initialized successfully");
        }
    }

    private void initializeCountryData() {
        if (countryRepo.count() == 0) {
            Eac eac = eacRepo.findAll().get(0);
            
            Country kenya = new Country();
            kenya.setName("Kenya");
            kenya.setIsCode("KE");
            kenya.setEmail("kenya@kra.go.ke");
            kenya.setEac(eac);
            countryRepo.save(kenya);
            
            Country uganda = new Country();
            uganda.setName("Uganda");
            uganda.setIsCode("UG");
            uganda.setEmail("uganda@ura.go.ug");
            uganda.setEac(eac);
            countryRepo.save(uganda);
            
            Country tanzania = new Country();
            tanzania.setName("Tanzania");
            tanzania.setIsCode("TZ");
            tanzania.setEmail("tanzania@tra.go.tz");
            tanzania.setEac(eac);
            countryRepo.save(tanzania);
            
            Country rwanda = new Country();
            rwanda.setName("Rwanda");
            rwanda.setIsCode("RW");
            rwanda.setEmail("rwanda@rra.gov.rw");
            rwanda.setEac(eac);
            countryRepo.save(rwanda);
            
            Country burundi = new Country();
            burundi.setName("Burundi");
            burundi.setIsCode("BI");
            burundi.setEmail("burundi@obr.bi");
            burundi.setEac(eac);
            countryRepo.save(burundi);

            Country zanzibar = new Country();
            zanzibar.setName("Zanzibar");
            zanzibar.setIsCode("TZ");
            zanzibar.setEmail("zanzibar@zra.go.tz");
            zanzibar.setEac(eac);
            countryRepo.save(zanzibar);

            Country southSudan = new Country();
            southSudan.setName("South Sudan");
            southSudan.setIsCode("SS");
            southSudan.setEmail("southsudan@rss.gov.ss");
            southSudan.setEac(eac);
            countryRepo.save(southSudan);

            System.out.println("Country data initialized successfully");
        }
    }

    private void initializeRevenueAuthorityData() {
        if (revenueAuthorityRepo.count() == 0) {
            // Get countries by name and use their entities
            Country kenya = countryRepo.findByName("Kenya");
            Country uganda = countryRepo.findByName("Uganda");
            Country tanzania = countryRepo.findByName("Tanzania");
            Country rwanda = countryRepo.findByName("Rwanda");
            Country burundi = countryRepo.findByName("Burundi");
            Country zanzibar = countryRepo.findByName("Zanzibar");
            Country southSudan = countryRepo.findByName("South Sudan");
            
            if (kenya != null) {
                RevenueAuthority kra = new RevenueAuthority();
                kra.setName("Kenya Revenue Authority");
                kra.setCountry(kenya);
                revenueAuthorityRepo.save(kra);
            }
            
            if (uganda != null) {
                RevenueAuthority ura = new RevenueAuthority();
                ura.setName("Uganda Revenue Authority");
                ura.setCountry(uganda);
                revenueAuthorityRepo.save(ura);
            }
            
            if (tanzania != null) {
                RevenueAuthority tra = new RevenueAuthority();
                tra.setName("Tanzania Revenue Authority");
                tra.setCountry(tanzania);
                revenueAuthorityRepo.save(tra);
            }
            
            if (rwanda != null) {
                RevenueAuthority rra = new RevenueAuthority();
                rra.setName("Rwanda Revenue Authority");
                rra.setCountry(rwanda);
                revenueAuthorityRepo.save(rra);
            }
            
            if (burundi != null) {
                RevenueAuthority obr = new RevenueAuthority();
                obr.setName("Office Burundais des Recettes");
                obr.setCountry(burundi);
                revenueAuthorityRepo.save(obr);
            }

            if (zanzibar != null) {
                RevenueAuthority zra = new RevenueAuthority();
                zra.setName("Zanzibar Revenue Authority");
                zra.setCountry(zanzibar);
                revenueAuthorityRepo.save(zra);
            }

            if (southSudan != null) {
                RevenueAuthority rss = new RevenueAuthority();
                rss.setName("Revenue Authority of South Sudan");
                rss.setCountry(southSudan);
                revenueAuthorityRepo.save(rss);
            }
            
            System.out.println("Revenue Authority data initialized successfully");
        }
    }

    private void initializeCommitteeData(){
        if(committeeRepo.count() == 0) {
            Committee committee = new Committee();
            committee.setName("Commissioner General");
            committeeRepo.save(committee); 
            System.out.println("Committee data initialized successfully");
        }
    }

    private void initializeSubCommitteeData(){
        if(subCommitteeRepo.count() == 0) {
            // Get the parent committee (Commissioner General)
            Committee parentCommittee = committeeRepo.findAll().get(0);
            
            SubCommittee subCommittee1 = new SubCommittee();
            subCommittee1.setName("Head Of Delegation");
            subCommittee1.setParentCommittee(parentCommittee);
            subCommitteeRepo.save(subCommittee1);

            SubCommittee subCommittee2 = new SubCommittee();
            subCommittee2.setName("Domestic Revenue Sub Committee");
            subCommittee2.setParentCommittee(parentCommittee);
            subCommitteeRepo.save(subCommittee2);

            SubCommittee subCommittee3 = new SubCommittee();
            subCommittee3.setName("Customs Revenue Sub Committee");
            subCommittee3.setParentCommittee(parentCommittee);
            subCommitteeRepo.save(subCommittee3);

            SubCommittee subCommittee4 = new SubCommittee();
            subCommittee4.setName("IT Sub Committee");
            subCommittee4.setParentCommittee(parentCommittee);
            subCommitteeRepo.save(subCommittee4);

            SubCommittee subCommittee5 = new SubCommittee();
            subCommittee5.setName("Legal Sub Committee");
            subCommittee5.setParentCommittee(parentCommittee);
            subCommitteeRepo.save(subCommittee5);

            SubCommittee subCommittee6 = new SubCommittee();
            subCommittee6.setName("HR Sub Committee");
            subCommittee6.setParentCommittee(parentCommittee);
            subCommitteeRepo.save(subCommittee6);

            SubCommittee subCommittee7 = new SubCommittee();
            subCommittee7.setName("Research Sub Committee");
            subCommittee7.setParentCommittee(parentCommittee);
            subCommitteeRepo.save(subCommittee7);

            System.out.println("Sub-Committee data initialized successfully");
        }
    }

    private void initializeCountryCommitteeMapping() {
        if (countryCommitteeRepo.count() == 0) {
            // Get the committee
            Committee committee = committeeRepo.findAll().get(0); // Commissioner General
            
            // Get all countries
            List<Country> countries = countryRepo.findAll();
            
            // Map each country to the committee
            for (Country country : countries) {
                CountryCommitte countryCommitte = new CountryCommitte();
                countryCommitte.setCommittee(committee);
                countryCommitte.setCountry(country);
                countryCommitteeRepo.save(countryCommitte);
            }
            
            System.out.println("Country-Committee mapping initialized successfully");
        }
    }

    private void initializeCountrySubCommitteeMapping() {
        if (countrySubCommitteeRepo.count() == 0) {
            // Get all sub-committees
            List<SubCommittee> subCommittees = subCommitteeRepo.findAll();
            
            // Get all countries
            List<Country> countries = countryRepo.findAll();
            
            // Map each country to all sub-committees
            for (Country country : countries) {
                for (SubCommittee subCommittee : subCommittees) {
                    CountrySubCommittee countrySubCommittee = new CountrySubCommittee();
                    countrySubCommittee.setSubCommittee(subCommittee);
                    countrySubCommittee.setCountry(country);
                    countrySubCommitteeRepo.save(countrySubCommittee);
                }
            }
            
            System.out.println("Country-SubCommittee mapping initialized successfully");
        }
    }

    private void initializeDefaultAdminUser() {
        if (userRepo.count() == 0) {
            User adminUser = new User();
            adminUser.setEmail("admin@earaconnect.com");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setName("System Administrator");
            adminUser.setRole(User.UserRole.ADMIN);
            adminUser.setActive(true);
            userRepo.save(adminUser);
            System.out.println("Default admin user initialized successfully");
        } else {
            // Fix any existing users with plaintext passwords (migration from NoOpPasswordEncoder)
            List<User> allUsers = userRepo.findAll();
            int migrated = 0;
            for (User user : allUsers) {
                if (user.getPassword() != null 
                        && !user.getPassword().startsWith("$2a$") 
                        && !user.getPassword().startsWith("$2b$")) {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    userRepo.save(user);
                    migrated++;
                }
            }
            if (migrated > 0) {
                System.out.println("Migrated " + migrated + " plaintext password(s) to BCrypt");
            }
        }
    }
}