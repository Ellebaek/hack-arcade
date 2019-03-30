package com.luminocityonside.arcadeapplication;

import com.amazonaws.auth.AWSCredentials;

public class AWSCredentialProvider implements AWSCredentials {

    @Override
    public String getAWSAccessKeyId() {
        return "AKIAJRGTSM2YU55OHHPA_this_account_is_closed";
    }

    @Override
    public String getAWSSecretKey() {
        return "ffhq8mjE/WNVeaQPVFiR9gpMwNyWrGcjlmCNnAx1_this_account_is_closed";
    }

}