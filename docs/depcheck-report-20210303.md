Pvm: 2021-03-03
Commit: 833eb124dd786a21b57f485d54dc14aa31b6cfd6

Vaatii ehkä toimenpiteitä:

* hibernate-core-5.4.15.Final.jar, hibernate-envers-5.4.15.Final.jar, hibernate-jcache-5.4.15.Final.jar, hibernate-jpamodelgen-5.4.15.Final.jar
 * CVE-2019-14900
   * SQL-injektio kyselyn SELECT ja GROUP BY -osisssa, jos käyttäjän syötettä ei eskapoida
   * korjattu 5.4.18:ssa, pitäisi ehkä päivittää varulta
 * CVE-2020-25638
    * SQL-injektio kyselyn kommenteissa, jos käyttäjän syötettä ei eskapoida
   * mukana vielä 5.4.23-finalissa, pitäisi selvittää ehjä versio
* keycloak-admin-client-10.0.2.jar, keycloak-common-10.0.2.jar, keycloak-core-10.0.2.jar
  * CVE-2020-10758
  * CVE-2020-10770
    * SSRF:n mahdollistava haavoittuvuus
  * CVE-2020-10776
    * XSS
  * CVE-2020-14302
    * Replay-hyökkäyksen mahdollistava haavoittuvuus
  * CVE-2020-14359
    * Autentikation ohitus sopivilla HTTP-otsikoilla. Ei välttämättä helppo hyödyntää, mutta kannattanee silti päivittää.
  * CVE-2020-14366
    * Path traversal, CVSS 7.5. Eiköhän tämäkin kannata päivittää pois.
  * CVE-2020-14389
    * Oikeushallintahaavoittuvuus
    * Korjattu 12.0.0
  * CVE-2020-1714
    * Puutteellinen validointi, josta voi seurata vaarallinen deserialisointi ja mielivaltaisen koodin ajaminen
    * Korjattu 11.0.0
  * CVE-2020-1725
    * Roolien muokkaus ei vaikuta välittömästi.
    * Korjattu 13.0.0
* kotlin-allopen-1.4.10.jar, kotlin-annotation-processing-gradle-1.4.10.jar
  * CVE-2020-29582
    * Lokaali hyökkääjä voi lukea tiedostoja ja hakemistoja koska niillä on väärät oikeudet.
* kotlin-compiler-embeddable-1.4.0.jar, kotlin-daemon-embeddable-1.4.0.jar, kotlin-reflect-1.4.0.jar, kotlin-reflect-1.4.10.jar, kotlin-script-runtime-1.4.0.jar, kotlin-script-runtime-1.4.10.jar, kotlin-stdlib-1.4.0.jar, kotlin-stdlib-1.4.10.jar, kotlin-stdlib-common-1.4.0.jar, kotlin-stdlib-common-1.4.10.jar, kotlin-stdlib-jdk7-1.4.10.jar, kotlin-stdlib-jdk8-1.4.10.jar, kotlin-test-1.4.10.jar, kotlin-test-annotations-common-1.4.10.jar, kotlin-test-common-1.4.10.jar, kotlin-test-junit-1.4.10.jar
  * CVE-2020-15824
    * Saattaa olla false positive, koska CVE on korjattu 1.4.0 releasessa (oli vielä RC:ssa)
  * CVE-2020-29582
    * Lokaali hyökkääjä voi lukea tiedostoja ja hakemistoja koska niillä on väärät oikeudet.
* kotlin-compiler-embeddable-1.4.10.jar, kotlin-daemon-embeddable-1.4.0.jar, kotlin-gradle-plugin-api-1.4.10.jar, kotlin-gradle-plugin-model-1.4.10.jar, kotlin-klib-commonizer-embeddable-1.4.10.jar, kotlin-noarg-1.4.10.jar, kotlin-scripting-common-1.4.10.jar, kotlin-scripting-compiler-embeddable-1.4.10.jar, kotlin-scripting-compiler-impl-embeddable-1.4.10.jar, kotlin-scripting-jvm-1.4.10.jar
  * CVE-2020-29582
    * Lokaali hyökkääjä voi lukea tiedostoja ja hakemistoja koska niillä on väärät oikeudet.
* nimbus-jose-jwt-7.8.1.jar
  * CVE-2019-17195
    * CVSS 9.8
    * JWT:n käsittely voi kaatua tai mahdollisesti jopa vuotaa tietoa

False positivet:

* jboss-websocket-api_1.1_spec-1.1.4.Final.jar
  * CVE-2020-11050
    * Näyttää false positivelta, koska CVE koskee https://tootallnate.github.io/Java-WebSocket/, eikä JBoss Websocket APIa.
    * Lisätietoja: https://github.com/TooTallNate/Java-WebSocket/issues/1019#issuecomment-628507934
* lang-tag-1.5.jar
  * CVE-2020-29242, CVE-2020-29243
    * False positive, väärin tunnistunut kirjasto (github.com/dhowden/tag)
* oauth2-oidc-sdk-6.14.jar
  * CVE-2007-1651
    * CSRF
    * False positive?
  * CVE-2007-1652
    * False positive?
* postgresql-1.14.3.jar
  * CVE-2007-2138, CVE-2010-0733, CVE-2014-0060, CVE-2014-0061, CVE-2014-0062, CVE-2014-0063, CVE-2014-0064, CVE-2014-0065, CVE-2014-0066, CVE-2014-0067, CVE-2014-8161, CVE-2015-0241, CVE-2015-0242, CVE-2015-0243, CVE-2015-0244, CVE-2015-3165, CVE-2015-3166, CVE-2015-3167, CVE-2015-5288, CVE-2015-5289, CVE-2016-0766, CVE-2016-0768, CVE-2016-0773, CVE-2016-5423, CVE-2016-5424, CVE-2016-7048, CVE-2017-14798, CVE-2017-7484, CVE-2018-1115, CVE-2019-10210, CVE-2019-10211, CVE-2020-25694, CVE-2020-25695
    * False positive, väärin tunnistettu

Ei välttämättä vaadi toimenpiteitä:

* checkstyle-8.18.jar
  * CVE-2019-10782
    * XXE
    * XML:ää ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
* commons-beanutils-1.9.3.jar
  * CVE-2019-10086
    * Deserialisointihaavoittuvuus
    * Deserialisoitavaa dataa ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
* guava-27.0.1-jre.jar, guava-29.0-jre.jar
  * CVE-2020-8908
    * com.google.common.io.Files.createTempDir():ssä haavoittuvuus, metodi ei käytössä
* httpclient-4.5.12.jar
  * CVE-2020-13956
    * Haavoittuvuus liittyy ikävästi muotoiltuun URIin. Koska näitä ei saada käyttäjältä, ei syytä toimenpiteisiin
    * Korjattu 5.4.13:ssa
* jackson-databind-2.10.4.jar
  * CVE-2020-25649
    * XXE
    * XML:ää ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
* junit-4.12.jar
  * CVE-2020-15250
    * Vaikuttaa vain testien aikana ja jos testejä ajavalla koneella on ei-luotettuja käyttäjiä
    * Korjattu 4.13.1:ssä
* log4j-api-2.11.2.jar, log4j-api-2.12.1.jar, log4j-to-slf4j-2.12.1.jar
  * CVE-2020-9488
    * Sertifikaattia ei validoida SMTP-appenderia käytettäessä. Ei käytetä SMTP-appenderia.
* nohttp-checkstyle-0.0.4.RELEASE.jar
  * CVE-2019-9658, CVE-2019-10782
    * XXE
    * XML:ää ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
* postgresql-42.2.12.jar
  * CVE-2020-13692
    * XXE
    * XML:ää ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
* resteasy-client-3.9.1.Final.jar, resteasy-jackson2-provider-3.9.1.Final.jar, resteasy-jaxb-provider-3.9.1.Final.jar, resteasy-jaxrs-3.9.1.Final.jar, resteasy-multipart-provider-3.9.1.Final.jar
  * CVE-2020-1695
    * Korjattu 3.12.0
    * Hankalahko hyväksikäyttää
  * CVE-2020-25633
    * Korjattu 4.5.6.Final:issa.
    * Hankalahko hyväksikäyttää
* snakeyaml-1.23.jar, snakeyaml-1.25.jar
  * CVE-2017-18640
    * DTD-entity expansion
    * Käytetäänkö ulkoisia DTD:itä ei-luotetuista lähteistä?
* spring-aop-5.1.5.RELEASE.jar, spring-core-5.1.5.RELEASE.jar
  * CVE-2020-5398
    * Vaatisi erikoisia ohjelmointivalintoja, erittäin epätodennäköinen
    * Korjattu 5.1.13
  * CVE-2020-5421
    * Korjattu 5.1.17
* spring-aop-5.2.6.RELEASE.jar, spring-aspects-5.2.6.RELEASE.jar, spring-beans-5.2.6.RELEASE.jar, spring-context-5.2.6.RELEASE.jar, spring-context-support-5.2.6.RELEASE.jar, spring-core-5.2.6.RELEASE.jar, spring-expression-5.2.6.RELEASE.jar, spring-jcl-5.2.6.RELEASE.jar, spring-jdbc-5.2.6.RELEASE.jar, spring-orm-5.2.6.RELEASE.jar, spring-test-5.2.6.RELEASE.jar, spring-tx-5.2.6.RELEASE.jar, spring-web-5.2.6.RELEASE.jar, spring-webmvc-5.2.6.RELEASE.jar
  * CVE-2020-5421
    * Korjattu 5.2.8
* spring-cloud-cloudfoundry-connector-2.0.7.RELEASE.jar (shaded: com.fasterxml.jackson.core:jackson-databind:2.10.0)
  * CVE-2020-25649
    * XXE
    * XML:ää ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
* spring-security-config-5.2.4.RELEASE.jar, spring-security-core-5.2.4.RELEASE.jar, spring-security-data-5.2.4.RELEASE.jar, spring-security-oauth2-client-5.2.4.RELEASE.jar, spring-security-oauth2-core-5.2.4.RELEASE.jar, spring-security-oauth2-jose-5.2.4.RELEASE.jar, spring-security-oauth2-resource-server-5.2.4.RELEASE.jar, spring-security-test-5.2.4.RELEASE.jar, spring-security-web-5.2.4.RELEASE.jar
  * CVE-2018-1258
    * Osuu jos Springistä on käytössä 5.0.5
  * CVE-2021-22112
    * Vaatii ohjelmointivirheen. Esimerkiksi tilanteissa, joissa käyttäjän oikeuksia korotetaan hetkellisesti. Ei taida osua meihin.
* undertow-core-2.0.30.Final.jar, undertow-servlet-2.0.30.Final.jar, undertow-websockets-jsr-2.0.30.Final.jar
  * CVE-2020-10687
    * Cache poisoning, XSS, tietovuoto
    * Korjattu 2.2.0.Final
  * CVE-2020-10705
    * Denial of service
    * Korjattu 2.1.1.Final
  * CVE-2020-10719
    * Korjattu 2.1.1.Final
    * HTTP request smuggling
  * CVE-2020-1757
    * Korjattu 2.0.30.SP1
  * CVE-2021-20220
    * Korjattu 2.2.0.Final, 2.1.6.Final ja 2.0.34.Final