package com.jiuyu.replay.common.tencent;

import com.tencentcloudapi.common.AbstractClient;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenRequest;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenResponse;

public class TencentStsClient extends AbstractClient {

    private static final String endpoint = "sts.tencentcloudapi.com";
    private static final String version = "2018-08-13";
    private static final String action = "GetFederationToken";

    public TencentStsClient(Credential credential, String region) {
        this(credential, region, new ClientProfile());
    }

    public TencentStsClient(Credential credential, String region, ClientProfile profile) {
        super(TencentStsClient.endpoint, TencentStsClient.version, credential, region, profile);
    }

    public GetFederationTokenResponse getFederationToken(GetFederationTokenRequest req) throws TencentCloudSDKException {
        req.setSkipSign(false);
        return this.internalRequest(req, action, GetFederationTokenResponse.class);
    }
}
