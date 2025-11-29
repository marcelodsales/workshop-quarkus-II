package com.redhat.service;

import com.redhat.monitoring.TrackDataAccess;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@TrackDataAccess
public class BankingServiceBaseEnhanced extends BankingServiceBase {

}
