package com.tomas.tenis.stats.config;

import com.tomas.tenis.stats.soap.CountryInfoService;
import com.tomas.tenis.stats.soap.CountryInfoServiceSoapType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SoapConfig {

    @Bean
    public CountryInfoService countryInfoService() {
        return new CountryInfoService();
    }

    @Bean
    public CountryInfoServiceSoapType countryInfoServiceSoapType(CountryInfoService service) {
        return service.getCountryInfoServiceSoap();
    }
}