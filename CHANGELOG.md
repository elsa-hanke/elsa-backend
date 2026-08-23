# Changelog

## [Unreleased](https://github.com/elsa-hanke/elsa-backend/compare/v2.2.2...HEAD)

- Elsa-1199 UnsupportedPdfCharactersException [`#696`](https://github.com/elsa-hanke/elsa-backend/pull/696)(22 August 2026)
- Replica profile [`#694`](https://github.com/elsa-hanke/elsa-backend/pull/694)(21 August 2026)
- Release/v2.2.x merge [`#693`](https://github.com/elsa-hanke/elsa-backend/pull/693)(14 August 2026)

## [v2.2.2](https://github.com/elsa-hanke/elsa-backend/compare/v2.2.1...v2.2.2) - 14 August 2026

## [v2.2.1](https://github.com/elsa-hanke/elsa-backend/compare/v2.2.0...v2.2.1) - 14 August 2026

## [v2.2.0-1](https://github.com/elsa-hanke/elsa-backend/compare/v2.1.5...v2.2.0-1) - 14 August 2026

- several improvements and refactorings focused on operational alerting for integration authentication failures [`#692`](https://github.com/elsa-hanke/elsa-backend/pull/692)(13 August 2026)
- ELSA-1194: Olemassa olevan vastuuhenkilön lisääminen erikoistuvan arvioijaksi tai kouluttajaksi ei enää laajenna hänen käyttöoikeuksiaan tahattomasti. [`#691`](https://github.com/elsa-hanke/elsa-backend/pull/691)(13 August 2026)
- feat: add Helsinki archiving integration configuration and tests [`#689`](https://github.com/elsa-hanke/elsa-backend/pull/689)(12 August 2026)
- ValmistumispyyntoServiceImpl testikattavuuden lisääminen [`#688`](https://github.com/elsa-hanke/elsa-backend/pull/688)(11 August 2026)
- chore: update CycloneDX BOM version to 3.4.0 and Cypress image to 15.20.0 in CI configuration [`#687`](https://github.com/elsa-hanke/elsa-backend/pull/687)(11 August 2026)
- fix: enable deprecation checks in detekt configuration and remove unnecessary suppressions [`#686`](https://github.com/elsa-hanke/elsa-backend/pull/686)(11 August 2026)
- chore: update Gradle wrapper to version 9.7.0 [`#685`](https://github.com/elsa-hanke/elsa-backend/pull/685)(11 August 2026)
- Added new Cypress E2E test case valmistumispyynto-yek.cy.ts [`#684`](https://github.com/elsa-hanke/elsa-backend/pull/684)(11 August 2026)
- Bump dompurify from 3.4.12 to 3.4.13 in /frontend [`#675`](https://github.com/elsa-hanke/elsa-backend/pull/675)(9 August 2026)
- Bump js-yaml from 3.15.0 to 3.15.1 in /frontend [`#676`](https://github.com/elsa-hanke/elsa-backend/pull/676)(9 August 2026)
- More e2e tests [`#674`](https://github.com/elsa-hanke/elsa-backend/pull/674)(8 August 2026)
- Detekt fixes PR1 [`#672`](https://github.com/elsa-hanke/elsa-backend/pull/672)(7 August 2026)
- bump detekt version and cleaned unused imports [`#671`](https://github.com/elsa-hanke/elsa-backend/pull/671)(7 August 2026)
- Reorganized unit tests [`#670`](https://github.com/elsa-hanke/elsa-backend/pull/670)(7 August 2026)
- yek-tyoskentelyjakso e2e test [`#667`](https://github.com/elsa-hanke/elsa-backend/pull/667)(6 August 2026)
- Enabled detekt UnusedImport rule [`#665`](https://github.com/elsa-hanke/elsa-backend/pull/665)(6 August 2026)
- Bump fast-uri from 3.1.4 to 3.1.5 in /frontend [`#658`](https://github.com/elsa-hanke/elsa-backend/pull/658)(5 August 2026)
- Refactors the handling of Terveyskeskuskoulutusjakso-related REST endpoints by introducing a shared base resource class to centralize exception handling and file download logic [`#657`](https://github.com/elsa-hanke/elsa-backend/pull/657)(5 August 2026)
- file download response standardization + test [`#655`](https://github.com/elsa-hanke/elsa-backend/pull/655)(5 August 2026)
- arviointityökalut e2e tests [`#654`](https://github.com/elsa-hanke/elsa-backend/pull/654)(5 August 2026)
- use infra/postgresql.yml instead of src/main/docker/postgresql.yml for starting and stopping the Postgres database [`#653`](https://github.com/elsa-hanke/elsa-backend/pull/653)(5 August 2026)
- More e2e tests [`#652`](https://github.com/elsa-hanke/elsa-backend/pull/652)(3 August 2026)
- More e2e tests [`#651`](https://github.com/elsa-hanke/elsa-backend/pull/651)(3 August 2026)
- ehcache and thumbnailator bump [`#650`](https://github.com/elsa-hanke/elsa-backend/pull/650)(2 August 2026)
- com.nhaarman.mockitokotlin2 -&gt; org.mockito.kotlin [`#649`](https://github.com/elsa-hanke/elsa-backend/pull/649)(2 August 2026)
- ELSA-1169 erikoistuvan-seuranta.cy.ts [`#632`](https://github.com/elsa-hanke/elsa-backend/pull/632)(30 July 2026)
- chore: update Gradle wrapper to version 9.6.1 [`#645`](https://github.com/elsa-hanke/elsa-backend/pull/645)(28 July 2026)
- Bump http-proxy-middleware from 2.0.9 to 2.0.10 in /frontend [`#633`](https://github.com/elsa-hanke/elsa-backend/pull/633)(27 July 2026)
- Bump js-yaml from 3.14.2 to 3.15.0 in /frontend [`#635`](https://github.com/elsa-hanke/elsa-backend/pull/635)(27 July 2026)
- Bump websocket-driver from 0.7.4 to 0.7.5 in /frontend [`#636`](https://github.com/elsa-hanke/elsa-backend/pull/636)(27 July 2026)
- Bump axios from 1.16.0 to 1.18.0 in /frontend [`#638`](https://github.com/elsa-hanke/elsa-backend/pull/638)(27 July 2026)
- Bump body-parser from 1.20.1 to 1.20.6 in /frontend [`#644`](https://github.com/elsa-hanke/elsa-backend/pull/644)(27 July 2026)
- Bump dompurify from 3.3.1 to 3.4.12 in /frontend [`#639`](https://github.com/elsa-hanke/elsa-backend/pull/639)(27 July 2026)
- Bump systeminformation from 5.31.6 to 5.31.17 in /e2e [`#637`](https://github.com/elsa-hanke/elsa-backend/pull/637)(27 July 2026)
- Bump svgo from 2.8.2 to 2.8.3 in /frontend [`#643`](https://github.com/elsa-hanke/elsa-backend/pull/643)(27 July 2026)
- Bump shell-quote from 1.8.4 to 1.10.0 in /frontend [`#641`](https://github.com/elsa-hanke/elsa-backend/pull/641)(27 July 2026)
- Bump fast-uri from 3.1.2 to 3.1.4 in /frontend [`#640`](https://github.com/elsa-hanke/elsa-backend/pull/640)(27 July 2026)
- bump spring boot and spring security [`#642`](https://github.com/elsa-hanke/elsa-backend/pull/642)(27 July 2026)
- change error logging to warning for incorrect login name and remove alert publishing [`#628`](https://github.com/elsa-hanke/elsa-backend/pull/628)(16 June 2026)
- enhance error logging for archiving failures with detailed HTTP response information [`#627`](https://github.com/elsa-hanke/elsa-backend/pull/627)(15 June 2026)
- fix: add error logging for unknown educational status in PeppiOulu data fetching service [`#625`](https://github.com/elsa-hanke/elsa-backend/pull/625)(15 June 2026)
- Release v2.1.5 merge [`#624`](https://github.com/elsa-hanke/elsa-backend/pull/624)(15 June 2026)
- update Kotlin and Detekt plugin versions to 2.4.0 and 2.0.0-alpha.4 [`#623`](https://github.com/elsa-hanke/elsa-backend/pull/623)(14 June 2026)
- Peppi and Sisu services: refactoring and reorganization [`#622`](https://github.com/elsa-hanke/elsa-backend/pull/622)(13 June 2026)
- More external integration tests [`#614`](https://github.com/elsa-hanke/elsa-backend/pull/614)(12 June 2026)
- enhance error handling for GraphQL responses by logging specific no value present errors [`#616`](https://github.com/elsa-hanke/elsa-backend/pull/616)(10 June 2026)
- Update minimum file size error message for document uploads [`#615`](https://github.com/elsa-hanke/elsa-backend/pull/615)(9 June 2026)
- Add alert publishing for failed user login due to incorrect name [`#612`](https://github.com/elsa-hanke/elsa-backend/pull/612)(9 June 2026)
- ELSA1-1145: lisälogitus tyoskentelyjakon asiakirjoihin [`#590`](https://github.com/elsa-hanke/elsa-backend/pull/590)(9 June 2026)
- Bump express from 4.18.2 to 4.22.2 in /frontend [`#603`](https://github.com/elsa-hanke/elsa-backend/pull/603)(8 June 2026)
- Bump tinymce from 7.2.0 to 7.9.3 in /frontend [`#610`](https://github.com/elsa-hanke/elsa-backend/pull/610)(8 June 2026)
- enable peppi Tampere externalIntegrationTests [`#611`](https://github.com/elsa-hanke/elsa-backend/pull/611)(8 June 2026)
- Bump launch-editor from 2.3.0 to 2.14.1 in /frontend [`#609`](https://github.com/elsa-hanke/elsa-backend/pull/609)(4 June 2026)
- Bump prismjs from 1.27.0 to 1.30.0 in /frontend [`#606`](https://github.com/elsa-hanke/elsa-backend/pull/606)(4 June 2026)
- Bump http-proxy-middleware from 2.0.4 to 2.0.9 in /frontend [`#607`](https://github.com/elsa-hanke/elsa-backend/pull/607)(3 June 2026)
- Bump js-yaml from 3.14.1 to 3.14.2 in /frontend [`#605`](https://github.com/elsa-hanke/elsa-backend/pull/605)(3 June 2026)
- Bump async from 2.6.3 to 2.6.4 in /frontend [`#601`](https://github.com/elsa-hanke/elsa-backend/pull/601)(3 June 2026)
- Bump @babel/helpers from 7.23.4 to 7.29.7 in /frontend [`#602`](https://github.com/elsa-hanke/elsa-backend/pull/602)(3 June 2026)
- fix integration login bug + update error handling + logging for Apollo 4 integration [`#588`](https://github.com/elsa-hanke/elsa-backend/pull/588)(3 June 2026)
- Add compile-time guards for spring-cloud-aws-starter-s3 dependency [`#600`](https://github.com/elsa-hanke/elsa-backend/pull/600)(2 June 2026)
- Revert "Remove unnecessary AWS S3 dependency and configuration (#597)" [`#599`](https://github.com/elsa-hanke/elsa-backend/pull/599)(1 June 2026)
- Remove unused frontend dependencies [`#596`](https://github.com/elsa-hanke/elsa-backend/pull/596)(1 June 2026)
- Remove unnecessary AWS S3 dependency and configuration [`#597`](https://github.com/elsa-hanke/elsa-backend/pull/597)(1 June 2026)
- Add management port and job triggering endpoint [`#594`](https://github.com/elsa-hanke/elsa-backend/pull/594)(31 May 2026)
- Bump axios from 1.15.2 to 1.16.0 in /frontend [`#593`](https://github.com/elsa-hanke/elsa-backend/pull/593)(31 May 2026)
- Add E2E tests for editing training periods and theory training [`#595`](https://github.com/elsa-hanke/elsa-backend/pull/595)(31 May 2026)
- Bump flatted from 3.2.5 to 3.4.2 in /frontend [`#592`](https://github.com/elsa-hanke/elsa-backend/pull/592)(29 May 2026)
- Enhance E2E tests for koejakso evaluation and approval process [`#591`](https://github.com/elsa-hanke/elsa-backend/pull/591)(29 May 2026)
- Add metrics for archiving and PDF generation operations [`#589`](https://github.com/elsa-hanke/elsa-backend/pull/589)(29 May 2026)
- Bump tmp from 0.2.5 to 0.2.7 in /e2e [`#587`](https://github.com/elsa-hanke/elsa-backend/pull/587)(28 May 2026)
- Add CloudWatch metrics export configuration and client bean [`#586`](https://github.com/elsa-hanke/elsa-backend/pull/586)(28 May 2026)
- Bump picomatch from 2.3.1 to 2.3.2 in /frontend [`#580`](https://github.com/elsa-hanke/elsa-backend/pull/580)(26 May 2026)
- Implement alert publishing for archiving failures and add SNS config [`#579`](https://github.com/elsa-hanke/elsa-backend/pull/579)(26 May 2026)
- Rename createWithoutOpintotietodata method and restrict usage only to development mode [`#576`](https://github.com/elsa-hanke/elsa-backend/pull/576)(24 May 2026)
- Bump svgo from 2.8.0 to 2.8.2 in /frontend [`#573`](https://github.com/elsa-hanke/elsa-backend/pull/573)(22 May 2026)
- ApolloClient version mismatch fix [`#575`](https://github.com/elsa-hanke/elsa-backend/pull/575)(22 May 2026)
- Bump rollup from 2.70.1 to 2.80.0 in /frontend [`#570`](https://github.com/elsa-hanke/elsa-backend/pull/570)(22 May 2026)
- Bump lodash from 4.17.23 to 4.18.1 in /e2e [`#571`](https://github.com/elsa-hanke/elsa-backend/pull/571)(22 May 2026)
- ELSAINSI-3 Detekt käyttöönotto [`#572`](https://github.com/elsa-hanke/elsa-backend/pull/572)(22 May 2026)
- ELSAINSI-37 Integraatioiden testaus [`#556`](https://github.com/elsa-hanke/elsa-backend/pull/556)(22 May 2026)
- Bump webpack from 5.71.0 to 5.106.2 in /frontend [`#566`](https://github.com/elsa-hanke/elsa-backend/pull/566)(19 May 2026)
- Bump sha.js from 2.4.11 to 2.4.12 in /frontend [`#567`](https://github.com/elsa-hanke/elsa-backend/pull/567)(18 May 2026)
- ELSAINSI-3 Detekt käyttöönotto [`#568`](https://github.com/elsa-hanke/elsa-backend/pull/568)(18 May 2026)
- Bump systeminformation from 5.31.5 to 5.31.6 in /e2e [`#558`](https://github.com/elsa-hanke/elsa-backend/pull/558)(16 May 2026)
- Bump lodash from 4.17.21 to 4.18.1 in /frontend [`#559`](https://github.com/elsa-hanke/elsa-backend/pull/559)(16 May 2026)
- chore: update Gradle to version 8.14.5 [`#557`](https://github.com/elsa-hanke/elsa-backend/pull/557)(16 May 2026)
- ELSAINSI-41 Sähköposti on lähetettävä vasta, kun transaktio on suoritettu onnistuneesti [`#555`](https://github.com/elsa-hanke/elsa-backend/pull/555)(11 May 2026)
- Release v2.1.x main merge [`#554`](https://github.com/elsa-hanke/elsa-backend/pull/554)(11 May 2026)
- expand e2e tests for koulutussopimus approval process [`#552`](https://github.com/elsa-hanke/elsa-backend/pull/552)(10 May 2026)
- enhance e2e tests for valmistumispyyntö approval process with verification tokens [`#551`](https://github.com/elsa-hanke/elsa-backend/pull/551)(8 May 2026)
- enhance approval logging with detailed information and error handling [`#546`](https://github.com/elsa-hanke/elsa-backend/pull/546)(6 May 2026)
- ELSA-1134: Lisäty logitusta, jotta nähdään mihin käsittely epäonnistuu [`#545`](https://github.com/elsa-hanke/elsa-backend/pull/545)(6 May 2026)

## [v2.1.5](https://github.com/elsa-hanke/elsa-backend/compare/v2.1.4...v2.1.5) - 12 June 2026

- Update feedback email and enhance file validation logging [`#621`](https://github.com/elsa-hanke/elsa-backend/pull/621)(12 June 2026)

## [v2.1.4](https://github.com/elsa-hanke/elsa-backend/compare/v2.1.3...v2.1.4) - 5 June 2026

- ELSA-1140 + e2e testit [`#608`](https://github.com/elsa-hanke/elsa-backend/pull/608)(5 June 2026)

## [v2.1.3](https://github.com/elsa-hanke/elsa-backend/compare/v2.1.2...v2.1.3) - 2 June 2026

## [v2.1.2](https://github.com/elsa-hanke/elsa-backend/compare/v2.1.1...v2.1.2) - 25 May 2026

- ELSA-1140:  poistettu filteröinti YEK-käyttäjältä [`#578`](https://github.com/elsa-hanke/elsa-backend/pull/578)(25 May 2026)

## [v2.1.1](https://github.com/elsa-hanke/elsa-backend/compare/v2.1.0...v2.1.1) - 7 May 2026

- ELSA-1134: korjattu testi toimimaan oikein (päivämäärä muotojen korjaus) [`#550`](https://github.com/elsa-hanke/elsa-backend/pull/550)(7 May 2026)
- ELSA-1134: xml konversiotesti arkistointiin vaiheeseen  [`#549`](https://github.com/elsa-hanke/elsa-backend/pull/549)(7 May 2026)
- ELSA-1135: jacksonVersion conflict fix + enhance error logging for exception handlers [`#548`](https://github.com/elsa-hanke/elsa-backend/pull/548)(7 May 2026)

## [v2.1.0](https://github.com/elsa-hanke/elsa-backend/compare/v2.0.1...v2.1.0) - 6 May 2026

- enhance approval logging with detailed information and error handling [`#546`](https://github.com/elsa-hanke/elsa-backend/pull/546)(6 May 2026)
- ELSA-1134: Lisäty logitusta, jotta nähdään mihin käsittely epäonnistuu [`#545`](https://github.com/elsa-hanke/elsa-backend/pull/545)(6 May 2026)
- Bump tinymce from 6.3.1 to 7.2.0 in /frontend [`#540`](https://github.com/elsa-hanke/elsa-backend/pull/540)(29 April 2026)
- Bump node-forge from 1.3.0 to 1.4.0 in /frontend [`#538`](https://github.com/elsa-hanke/elsa-backend/pull/538)(28 April 2026)
- Enable CI workflows for dependabot branches and update Cypress to 15.14.0 [`#539`](https://github.com/elsa-hanke/elsa-backend/pull/539)(28 April 2026)
- Refactor changelog generation to commit directly to main and add nightly changelog update workflow [`#537`](https://github.com/elsa-hanke/elsa-backend/pull/537)(28 April 2026)
- Release v2.0.1 merge [`#535`](https://github.com/elsa-hanke/elsa-backend/pull/535)(28 April 2026)
- Add E2E test cases and database tasks for koejakso evaluations [`#534`](https://github.com/elsa-hanke/elsa-backend/pull/534)(27 April 2026)
- Update dependencies to address vulnerabilities  [`#536`](https://github.com/elsa-hanke/elsa-backend/pull/536)(27 April 2026)
- Cognito korvaa nyt accessKey:a kehitysympäristössä [`#533`](https://github.com/elsa-hanke/elsa-backend/pull/533)(22 April 2026)
- CSRF 403 virheen korjaus (axios v0 -&gt; v1) [`#531`](https://github.com/elsa-hanke/elsa-backend/pull/531)(21 April 2026)

## [v2.0.1](https://github.com/elsa-hanke/elsa-backend/compare/v2.0.0...v2.0.1) - 21 April 2026

- CSRF 403 virheen korjaus (axios v0 -&gt; v1) (#531) [`#532`](https://github.com/elsa-hanke/elsa-backend/pull/532)(21 April 2026)

## [v2.0.0](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.16...v2.0.0) - 21 April 2026

- Release/v0.0.x merge [`#530`](https://github.com/elsa-hanke/elsa-backend/pull/530)(21 April 2026)
- ELSA-1127 Valmistumispyynnön hyväksyminen ei onnistu [`#524`](https://github.com/elsa-hanke/elsa-backend/pull/524)(20 April 2026)

## [v0.0.16](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.15...v0.0.16) - 21 April 2026

## [v0.0.15](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.14...v0.0.15) - 21 April 2026

## [v0.0.14](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.13...v0.0.14) - 21 April 2026

## [v0.0.13](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.12...v0.0.13) - 20 April 2026

## [v0.0.12](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.11...v0.0.12) - 20 April 2026

## [v0.0.11](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.10...v0.0.11) - 20 April 2026

## [v0.0.10](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.9...v0.0.10) - 20 April 2026

- Add versioning to landing page and display app version in footer [`#529`](https://github.com/elsa-hanke/elsa-backend/pull/529)(20 April 2026)
- Remove unnecessary Testcontainers dependency and update random ID generation method [`#528`](https://github.com/elsa-hanke/elsa-backend/pull/528)(19 April 2026)
- Add MDC filter to log authenticated user ID for request correlation [`#527`](https://github.com/elsa-hanke/elsa-backend/pull/527)(19 April 2026)
- Bump Thymeleaf 3.1.4 [`#526`](https://github.com/elsa-hanke/elsa-backend/pull/526)(18 April 2026)
- Revert "Revert "Bump axios to version 1.15.0 (#522)" (#523)" [`#525`](https://github.com/elsa-hanke/elsa-backend/pull/525)(17 April 2026)
- Revert "Bump axios to version 1.15.0 (#522)" [`#523`](https://github.com/elsa-hanke/elsa-backend/pull/523)(15 April 2026)
- Bump axios to version 1.15.0 [`#522`](https://github.com/elsa-hanke/elsa-backend/pull/522)(15 April 2026)

## [v0.0.9](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.8...v0.0.9) - 14 April 2026

## [v0.0.8](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.7...v0.0.8) - 14 April 2026

## [v0.0.7](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.6...v0.0.7) - 14 April 2026

## [v0.0.6](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.5...v0.0.6) - 14 April 2026

## [v0.0.5](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.4...v0.0.5) - 14 April 2026

## [v0.0.4](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.3...v0.0.4) - 14 April 2026

## [v0.0.3](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.2...v0.0.3) - 14 April 2026

## [v0.0.2](https://github.com/elsa-hanke/elsa-backend/compare/v0.0.1...v0.0.2) - 14 April 2026

## v0.0.1 - 14 April 2026

- ELSAINSI-15 Frontend & backend repojen yhdistys [`#521`](https://github.com/elsa-hanke/elsa-backend/pull/521)(14 April 2026)
- E2e tests [`#519`](https://github.com/elsa-hanke/elsa-backend/pull/519)(9 April 2026)
- ELSAINSI-8 CI/CD rungon muutokset [`#516`](https://github.com/elsa-hanke/elsa-backend/pull/516)(29 March 2026)
- ELSAINSI-10 Tietoturva haavoittuvuudet (backend) [`#517`](https://github.com/elsa-hanke/elsa-backend/pull/517)(25 March 2026)
- bump gradle v7 -&gt; v8, added grype security scan, spring-boot bump 3.4.1 -&gt; 3.4.13 [`#502`](https://github.com/elsa-hanke/elsa-backend/pull/502)(23 March 2026)
- Revert "Revert "Update AWS region configuration in application.yml (#501)" (#…" [`#515`](https://github.com/elsa-hanke/elsa-backend/pull/515)(23 March 2026)
- Revert "Revert "Lisätty yksikkötestejä laajasti: FileValidationService, Local…" [`#514`](https://github.com/elsa-hanke/elsa-backend/pull/514)(23 March 2026)
- Revert "Revert "The Build and test step has been split into three distinct st…" [`#513`](https://github.com/elsa-hanke/elsa-backend/pull/513)(23 March 2026)
- Revert "Revert "com.adarshr.test-logger adds test results in CI/CD (#499)" (#…" [`#512`](https://github.com/elsa-hanke/elsa-backend/pull/512)(23 March 2026)
- Revert "Revert "Refactor OkHttpClient initialization to use Sisu certificates…" [`#511`](https://github.com/elsa-hanke/elsa-backend/pull/511)(23 March 2026)
- Revert "com.adarshr.test-logger adds test results in CI/CD (#499)" [`#507`](https://github.com/elsa-hanke/elsa-backend/pull/507)(5 March 2026)
- Revert "The Build and test step has been split into three distinct steps: Bui…" [`#508`](https://github.com/elsa-hanke/elsa-backend/pull/508)(5 March 2026)
- Revert "Refactor OkHttpClient initialization to use Sisu certificates" [`#506`](https://github.com/elsa-hanke/elsa-backend/pull/506)(5 March 2026)
- Revert "Update AWS region configuration in application.yml (#501)" [`#509`](https://github.com/elsa-hanke/elsa-backend/pull/509)(5 March 2026)
- Revert "Lisätty yksikkötestejä laajasti: FileValidationService, LocalDateExte…" [`#510`](https://github.com/elsa-hanke/elsa-backend/pull/510)(5 March 2026)
- Lisätty yksikkötestejä laajasti: FileValidationService, LocalDateExtensions, DoubleExtensions, PeriodExtensions, MultipartFileExtensions, SecurityUtils ja NimiErikoisalaAndAvoinCriteria [`#505`](https://github.com/elsa-hanke/elsa-backend/pull/505)(27 February 2026)
- Update AWS region configuration in application.yml [`#501`](https://github.com/elsa-hanke/elsa-backend/pull/501)(12 February 2026)
- The Build and test step has been split into three distinct steps: Build (skipping tests), Test, and Integration test [`#500`](https://github.com/elsa-hanke/elsa-backend/pull/500)(10 February 2026)
- com.adarshr.test-logger adds test results in CI/CD [`#499`](https://github.com/elsa-hanke/elsa-backend/pull/499)(10 February 2026)
- new textarea for virkailija in valmistumisen tarkistus [`#491`](https://github.com/elsa-hanke/elsa-backend/pull/491)(17 December 2025)
- New textarea for virkailija koejakson vastuuhenkilon arvio [`#490`](https://github.com/elsa-hanke/elsa-backend/pull/490)(15 December 2025)
- Fix delete suoritusarviointi [`#488`](https://github.com/elsa-hanke/elsa-backend/pull/488)(30 October 2025)
- ELSA-981: poista ensin valtuutukset [`#487`](https://github.com/elsa-hanke/elsa-backend/pull/487)(27 October 2025)
- Adding substituded to valid opinto-oikeus state [`#485`](https://github.com/elsa-hanke/elsa-backend/pull/485)(20 October 2025)
- ELSA-1004: Arviointityokalu asiakirjan lataus ei toimi [`#482`](https://github.com/elsa-hanke/elsa-backend/pull/482)(2 October 2025)
- ELSA-911: Korjattu nimen automaattinen päivitys suomi.fi tiedon muuttuessa [`#479`](https://github.com/elsa-hanke/elsa-backend/pull/479)(11 September 2025)
- ELSA-989: Korjaus arviointityokalujen esittely vastuuhenkilöllä [`#477`](https://github.com/elsa-hanke/elsa-backend/pull/477)(2 September 2025)
- ELSA-949: Päivitetty Postgres versio 13 --&gt; 16 [`#472`](https://github.com/elsa-hanke/elsa-backend/pull/472)(25 August 2025)
- ELSA-979: arkistointi kaytossa tiedon tarkastus ennen avainten latausta [`#473`](https://github.com/elsa-hanke/elsa-backend/pull/473)(19 August 2025)
- ELSA-926: palauta luonnos tilaan [`#471`](https://github.com/elsa-hanke/elsa-backend/pull/471)(27 June 2025)
- ELSA-926: Arviointityokalu korjauksia ja jatkokehityksiä [`#470`](https://github.com/elsa-hanke/elsa-backend/pull/470)(18 June 2025)
- Palaute mailin otsikko muutos [`#469`](https://github.com/elsa-hanke/elsa-backend/pull/469)(6 June 2025)
- ELSA-911: Tarkistetaan käyttäjän nimi kirjautumisen yhteydessä [`#468`](https://github.com/elsa-hanke/elsa-backend/pull/468)(2 June 2025)
- ELSA-922: Nostettu Postgresql version 12.3 --&gt; 13.16 (sama kuin AWS) [`#464`](https://github.com/elsa-hanke/elsa-backend/pull/464)(13 May 2025)
- ELSA-1616: keskeneräisenä tallennus validoinnin ohitus suoritusarviointiin [`#463`](https://github.com/elsa-hanke/elsa-backend/pull/463)(7 April 2025)
- ELSA-775: Korjattu otsikon ääkköset [`#462`](https://github.com/elsa-hanke/elsa-backend/pull/462)(4 April 2025)
- ELSA-890: Vaihdettu email!! --&gt; email.toString(), joka tarkoittaa, että vaikka email = null, mäppäys ei hajoa, mutta emailina palautuu null tekstinä [`#461`](https://github.com/elsa-hanke/elsa-backend/pull/461)(3 April 2025)
- ELSA-1604: Arviointityokalut (Arvioija) [`#459`](https://github.com/elsa-hanke/elsa-backend/pull/459)(31 March 2025)
- ELSA-1601: Arviointityokalut (tekninen pääkäyttäjä) [`#455`](https://github.com/elsa-hanke/elsa-backend/pull/455)(18 March 2025)
- ELSA-864: Tarjotaan myös null authoriteetillä olevia erikoistujien yhdistämiseen. [`#457`](https://github.com/elsa-hanke/elsa-backend/pull/457)(12 March 2025)
- Vaihdetaan palaute osoite [`#458`](https://github.com/elsa-hanke/elsa-backend/pull/458)(12 March 2025)
- ELSA-775 Muistutus päättyvästä opinto-oikeudesta erikoistuvalle [`#456`](https://github.com/elsa-hanke/elsa-backend/pull/456)(7 March 2025)
- ELSA-822: Tuleva opinto-oikeus jumittaa tilin [`#451`](https://github.com/elsa-hanke/elsa-backend/pull/451)(3 March 2025)
- fix csv [`#454`](https://github.com/elsa-hanke/elsa-backend/pull/454)(28 February 2025)
- muutoksia heratteet ja tekstit [`#452`](https://github.com/elsa-hanke/elsa-backend/pull/452)(27 February 2025)
- Revert "PageImpl -&gt; PagedModel konversio" [`#444`](https://github.com/elsa-hanke/elsa-backend/pull/444)(11 February 2025)
- Ei päivitetä NVD cachea joka ajolla, koska API ei ole stabiili [`#433`](https://github.com/elsa-hanke/elsa-backend/pull/433)(3 December 2024)
- ELSA-511 [`#432`](https://github.com/elsa-hanke/elsa-backend/pull/432)(2 December 2024)
- Elsa 438 [`#431`](https://github.com/elsa-hanke/elsa-backend/pull/431)(18 November 2024)
- ELSA-718 yek valmistumispyynnön heräte korjaus [`#423`](https://github.com/elsa-hanke/elsa-backend/pull/423)(28 October 2024)
- Elsa 397 lisataan yek koulutettava tilien yhdistamiseen [`#422`](https://github.com/elsa-hanke/elsa-backend/pull/422)(23 October 2024)
- Jib plugin version päivitys [`#421`](https://github.com/elsa-hanke/elsa-backend/pull/421)(22 October 2024)
- ELSA-675: YEK ei vaikuta koejakson valmistumiseen liittyviin muihin opinto-oikeuksiin [`#420`](https://github.com/elsa-hanke/elsa-backend/pull/420)(14 October 2024)
- ELSA-397: Lisätty loput puuttuvat koejaksotaulujen tietojen siirrot tilien yhdistämiseen [`#419`](https://github.com/elsa-hanke/elsa-backend/pull/419)(11 October 2024)
- YEK-1595: Lisätty virkailijalle oikeus muuttaa laillistamispäivä polun tietoja [`#417`](https://github.com/elsa-hanke/elsa-backend/pull/417)(8 October 2024)
- ELSA-397: Lisää korjauksia [`#414`](https://github.com/elsa-hanke/elsa-backend/pull/414)(8 October 2024)
- YEK-1595: Lääkarikoulutus suoritettu muutokset [`#416`](https://github.com/elsa-hanke/elsa-backend/pull/416)(8 October 2024)
- ELSA-397: käyttäjät asetettiin väärin [`#413`](https://github.com/elsa-hanke/elsa-backend/pull/413)(4 October 2024)
- ELSA-397: tilien yhdistaminen virkailija [`#412`](https://github.com/elsa-hanke/elsa-backend/pull/412)(3 October 2024)
- Github actions ecs service name fix [`#411`](https://github.com/elsa-hanke/elsa-backend/pull/411)(3 October 2024)
- ELSA-397: Kayttajatilien yhdistaminen [`#410`](https://github.com/elsa-hanke/elsa-backend/pull/410)(30 September 2024)
- yek-1594-oulu-asetuksen-muokkaus [`#408`](https://github.com/elsa-hanke/elsa-backend/pull/408)(27 September 2024)
- YEK-1590: YEK pois vastuuhenkilön tehtävien erikoisalalistauksesta [`#405`](https://github.com/elsa-hanke/elsa-backend/pull/405)(16 September 2024)
- Disable PR triggers to avoid duplicate pipeline runs [`#401`](https://github.com/elsa-hanke/elsa-backend/pull/401)(6 September 2024)
- YEK-1541: funktion nimi väärin [`#399`](https://github.com/elsa-hanke/elsa-backend/pull/399)(5 September 2024)
- YEK-1581: YEK valmistumispyynnön tekstikorjauksia [`#397`](https://github.com/elsa-hanke/elsa-backend/pull/397)(5 September 2024)
- YEK-1541: poissaolon syyt julkinen listaus mahdollisuus [`#393`](https://github.com/elsa-hanke/elsa-backend/pull/393)(26 August 2024)
- YEK-1578: Muutoksia työkertymän laskemiseen YEK puolelle [`#388`](https://github.com/elsa-hanke/elsa-backend/pull/388)(14 June 2024)
- YEK-1547: Ensimmäinen työskentelyjakso korjauksia [`#387`](https://github.com/elsa-hanke/elsa-backend/pull/387)(7 June 2024)
- YEK-1574: 9kk YEK tk-jaksojen tarkistus ja hyväksyntä [`#385`](https://github.com/elsa-hanke/elsa-backend/pull/385)(6 June 2024)
- YEK-1560: virkailija etusivu rajapinta muutokset [`#373`](https://github.com/elsa-hanke/elsa-backend/pull/373)(15 April 2024)
- YEK-1527: Valmistumispyynto kehityksiä [`#372`](https://github.com/elsa-hanke/elsa-backend/pull/372)(3 April 2024)
- ELSA-439 / Lomake E herate virkailijalle [`#371`](https://github.com/elsa-hanke/elsa-backend/pull/371)(25 March 2024)
- YEK-1537 [`#370`](https://github.com/elsa-hanke/elsa-backend/pull/370)(21 March 2024)
- YEK-1546-1547 [`#369`](https://github.com/elsa-hanke/elsa-backend/pull/369)(21 March 2024)
- ELSA-495: Erikoisala aktiiviseksi migraatio [`#368`](https://github.com/elsa-hanke/elsa-backend/pull/368)(19 March 2024)
- elsa-498-palaute-email-muutos [`#367`](https://github.com/elsa-hanke/elsa-backend/pull/367)(19 March 2024)
- YEK-1551: Teoriakoulutukset yek rajapinta [`#366`](https://github.com/elsa-hanke/elsa-backend/pull/366)(18 March 2024)
- elsa-443-koulutussopimukseen-vastuuhenkilon-nimi [`#363`](https://github.com/elsa-hanke/elsa-backend/pull/363)(13 March 2024)
- Elsa 436 tk jakson tyokertyman raja [`#364`](https://github.com/elsa-hanke/elsa-backend/pull/364)(13 March 2024)
- YEK-1517/1510: Asiakirjat ja laillistamisdokumentti [`#365`](https://github.com/elsa-hanke/elsa-backend/pull/365)(13 March 2024)
- ELSA-434: Lisää poissaolon syitä migraatio [`#360`](https://github.com/elsa-hanke/elsa-backend/pull/360)(29 February 2024)
- YEK-1544: YEK rooli ja työskentely jaksot  [`#359`](https://github.com/elsa-hanke/elsa-backend/pull/359)(28 February 2024)
- YEK-1542: Lisätty yek erikoisala migraatio [`#358`](https://github.com/elsa-hanke/elsa-backend/pull/358)(21 February 2024)
- ELSA-341: Koejakson loppukeskusteluun paattymispäivä [`#357`](https://github.com/elsa-hanke/elsa-backend/pull/357)(9 February 2024)
- ELSA-381: vaihdettu palaute osoite [`#355`](https://github.com/elsa-hanke/elsa-backend/pull/355)(16 January 2024)
- ELSA-328: Muuta opintosuoritus hakua osakokonaisuuksiin liittyen [`#351`](https://github.com/elsa-hanke/elsa-backend/pull/351)(15 December 2023)
- ELSA-376: lasketaan spring boot versio toimivaan [`#354`](https://github.com/elsa-hanke/elsa-backend/pull/354)(14 December 2023)
- ELSA-297: nostettu riippuvuus versioita [`#353`](https://github.com/elsa-hanke/elsa-backend/pull/353)(4 December 2023)
- ELSA-297: Seuranta listan erikoistuvien duplikaatit sivujen välillä [`#352`](https://github.com/elsa-hanke/elsa-backend/pull/352)(4 December 2023)
- Uoelsa 1045 image repojen siirto shared [`#189`](https://github.com/elsa-hanke/elsa-backend/pull/189)(13 April 2022)
