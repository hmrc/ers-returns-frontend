
# End of year returns service frontend

Employment Related Securities (ERS) is a microservice that allows employers to register their employee share (and other security) schemes with HMRC. The service allows customers to upload their ERS annual return files and check for formatting errors.

## Info

This project is a Scala web application using [code scaffolds](https://github.com/hmrc/hmrc-frontend-scaffold.g8)

This service uses upscan

## Running the service

Service Manager: `sm2 --start ERS_RETURNS_ALL`

Start local tests: `./run_all_tests.sh`

For the accessibility tests Node v12 or above is needed. Details can be found [here](https://github.com/hmrc/sbt-accessibility-linter).

Run the tests with command:
```
sbt clean A11y/test
```

| Repositories      | Link                                                 |
|-------------------|------------------------------------------------------|
| Backend           | https://github.com/hmrc/ers-returns                  |
| Stub              | https://github.com/hmrc/ers-eoy-return-stub          |
| Performance tests | https://github.com/hmrc/ers-returns-perf-tests       |
| Acceptance tests  | https://github.com/hmrc/ers-returns-acceptance-tests |

## Routes

Start the service locally by going to http://localhost:9949/auth-login-stub/gg-sign-in

Fill in the correct credentials:
- Credential Strength: Strong
- Confidence Level: 50
- Affinity Group: Organisation
- EPAYE: 1234/GA4567
  
  Select Submit

| *Url*                                                                                                                                                                                           | *Description*                                              |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XP1100000000350&schemeType=CSOP&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D  | Submit your Company Share Option Plan annual return        |
| http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XQ1100000000351&schemeType=SAYE&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D  | Submit your Save As You Earn annual return                 |
| http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XR1100000000352&schemeType=OTHER&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D | Submit your Other annual return                            |
| http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XS1100000000353&schemeType=SIP&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D   | Submit your Share Incentive Plan annual return             |
| http://localhost:9290/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2015%2F16&ersSchemeRef=XT1100000000354&schemeType=EMI&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D   | Submit your Enterprise Management Incentives annual return |

### Testing in QA
Due to the nature of the connection to DES in QA, only a collection of Scheme References can be used for a successful submission. Using an invalid Scheme Reference will result in a global error page when submitting the return. The current supported Scheme References are as follows:

| *Url*                                                                                                                                                                                                       | *Description*                                              |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| https://www.qa.tax.service.gov.uk/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2014%2F15&ersSchemeRef=XP1100000000350&schemeType=CSOP&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D  | Submit your Company Share Option Plan annual return        |
| https://www.qa.tax.service.gov.uk/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2014%2F15&ersSchemeRef=XQ1100000000351&schemeType=SAYE&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D  | Submit your Save As You Earn annual return                 |
| https://www.qa.tax.service.gov.uk/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2014%2F15&ersSchemeRef=XR1100000000352&schemeType=OTHER&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D | Submit your Other annual return                            |
| https://www.qa.tax.service.gov.uk/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2014%2F15&ersSchemeRef=XS1100000000353&schemeType=SIP&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D   | Submit your Share Incentive Plan annual return             |
| https://www.qa.tax.service.gov.uk/submit-your-ers-annual-return?aoRef=123PA12345678&taxYear=2014%2F15&ersSchemeRef=XT1100000000354&schemeType=EMI&schemeName=MyScheme&hmac=qlQmNGgreJRqJroWUUu0MxLq2oo%3D   | Submit your Enterprise Management Incentives annual return |


#### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
