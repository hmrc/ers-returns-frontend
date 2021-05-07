
# End of year returns service frontend

Employment Related Securities (ERS) is a microservice that allows employers to register their employee share (and other security) schemes with HMRC. The service allows customers to upload their ERS annual return files and check for formatting errors.

## Info

This project is a Scala web application using [code scaffolds](https://github.com/hmrc/hmrc-frontend-scaffold.g8)

This service uses upscan

## Running the service

Service Manager: ERS_RETURNS_ALL

|Repositories|Link|
|------------|----|
|Backend|https://github.com/hmrc/ers-returns|
|Stub|https://github.com/hmrc/ers-eoy-return-stub|
|Performance tests|https://github.com/hmrc/ers-returns-perf-tests|
|Acceptance tests|https://github.com/hmrc/ers-returns-acceptance-tests|

## Routes
-------
Start the service locally by going to http://localhost:9949/auth-login-stub/gg-sign-in

Fill in the correct credentials:
- Credential Strength: Strong
- Confidence Level: 50
- Affinity Group: Organisation
- EPAYE: 1234/GA4567
  
  Select Submit

| *Url* | *Description* |
|-------|---------------|
|http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XP1100000000350&schemeType=CSOP&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D|Submit your Company Share Option Plan annual return|
|http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XQ1100000000351&schemeType=SAYE&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D |Submit your Save As You Earn annual return|
|http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XR1100000000352&schemeType=OTHER&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D|Submit your Other annual return|
|http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XS1100000000353&schemeType=SIP&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D|Submit your Share Incentive Plan annual return|
|http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XT1100000000354&schemeType=EMI&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D|Submit your Enterprise Management Incentives annual return|

#### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
