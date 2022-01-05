Pvm: 2021-03-28

# Vaatii ehkä toimenpiteitä

* spring-security-core-5.5.0.jar
    * CVE-2021-22119
        * DoS
        * Mahdollisesti helppo hyödyntää
        * Korjattu 5.5.1
* hibernate-envers-5.4.15.Final.jar, hibernate-jcache-5.4.15.Final.jar, hibernate-jpamodelgen-5.4.15.Final.jar
* CVE-2019-14900
    * SQL-injektio kyselyn SELECT ja GROUP BY -osisssa, jos käyttäjän syötettä ei eskapoida
    * korjattu 5.4.18:ssa, pitäisi ehkä päivittää varulta
* CVE-2020-25638
    * SQL-injektio kyselyn kommenteissa, jos käyttäjän syötettä ei eskapoida
    * korjattu 5.4.24:ssä
* keycloak-admin-client-10.0.2.jar, keycloak-common-10.0.2.jar, keycloak-core-10.0.2.jar
    * CVE-2020-10758
        * DoS
        * korjattu 11.0.1:ssä
    * CVE-2020-10770
        * SSRF:n mahdollistava haavoittuvuus
        * korjattu 13.0.0:ssa
    * CVE-2020-10776
        * XSS
        * Korjattu 12.0.0:ssa
    * CVE-2020-14302
        * Replay-hyökkäyksen mahdollistava haavoittuvuus
    * CVE-2020-14366
        * Path traversal, CVSS 7.5. Eiköhän tämäkin kannata päivittää pois.
        * Korjattu 12.0.0:ssa
    * CVE-2020-14389
        * Oikeushallintahaavoittuvuus
        * Korjattu 12.0.0
    * CVE-2020-1714
        * Puutteellinen validointi, josta voi seurata vaarallinen deserialisointi ja mielivaltaisen koodin ajaminen
        * Korjattu 11.0.0
    * CVE-2020-1725
        * Roolien muokkaus ei vaikuta välittömästi.
        * Korjattu 13.0.0
    * CVE-2020-27826 TODO
        * Korjattu 12.0.0
    * CVE-2020-27838 TODO
        * Korjattu 13.0.0
* json-smart-2.3.jar
    * CVE-2021-27568
    * Saattaa aiheuttaa kaatumisen tai tietovuodon
    * Kannattaisi ehkä päivittää uusimpaan (tällä hetkellä 2.4.7)

# False positivet

* kotlin-compiler-embeddable-1.4.0.jar, kotlin-daemon-embeddable-1.4.0.jar, kotlin-reflect-1.4.0.jar,
  kotlin-reflect-1.4.10.jar, kotlin-script-runtime-1.4.0.jar, kotlin-script-runtime-1.4.10.jar, kotlin-stdlib-1.4.0.jar,
  kotlin-stdlib-1.4.10.jar, kotlin-stdlib-common-1.4.0.jar, kotlin-stdlib-common-1.4.10.jar,
  kotlin-stdlib-jdk7-1.4.10.jar, kotlin-stdlib-jdk8-1.4.10.jar, kotlin-test-1.4.10.jar,
  kotlin-test-annotations-common-1.4.10.jar, kotlin-test-common-1.4.10.jar, kotlin-test-junit-1.4.10.jar
    * CVE-2020-15824
        * False positive, korjattu 1.4.0 releasessa (oli vielä RC:ssa)
* postgresql-1.14.3.jar
    * CVE-2021-10127, CVE-2019-10128
        * False positive, ei käytetä ko. Windows-installereita
    * CVE-2021-3393
        * False positive, ei ajeta PostgreSQL-palvelinta
* commons-collections-3.2.1.jar
    * CVE-2015-6420
        * Koskee tiettyjä Ciscon tuotteita, ei meitä
    * CVE-2015-15708
        * Koskee Apache Synapsea, ei käytössä
* undertow-websockets-jsr-2.2.8.Final.jar
    * CVE-2021-33880
        * False positive, väärin tunnistettu
* spring-security-config-5.5.0.jar, spring-security-core-5.5.0.jar, spring-security-crypto-5.5.0.jar,
  spring-security-data-5.2.4.RELEASE.jar, spring-security-saml2-service-provider-5.5.0.jar,
  spring-security-test-5.2.4.RELEASE.jar, spring-security-web-5.5.0.jar
    * CVE-2018-1258
        * Osuu vain jos Springistä on käytössä 5.0.5

# Ei välttämättä vaadi toimenpiteitä

* bcprov-jdk15on-1.59.jar
    * CVE-2018-1000180
        * Korjattu 1.60
        * Avainparien generointi voi olla puutteellista
        * Epätodennäköinen
    * CVE-2018-1000613
        * Deserialisointibugi
        * Vaatisi hyökkääjän rakentaman privaattiavaimen, joka luetaan classpathista. Melko epätodennäköinen.
        * Korjattu 1.60
    * CVE-2020-26939
        * Korjattu 1.61
        * Ei dekryptata käyttäjän antamaa syötettä, joten hyvin epätodennäköinen
* kotlin-allopen-1.4.10.jar, kotlin-annotation-processing-gradle-1.4.10.jar, kotlin-compiler-embeddable-1.4.0.jar,
  kotlin-compiler-embeddable-1.4.10.jar, kotlin-daemon-embeddable-1.4.0.jar, kotlin-daemon-embeddable-1.4.10.jar,
  kotlin-gradle-plugin-api-1.4.10.jar, kotlin-gradle-plugin-model-1.4.10.jar,
  kotlin-klib-commonizer-embeddable-1.4.10.jar, kotlin-noarg-1.4.10.jar, kotlin-reflect-1.4.0.jar,
  kotlin-reflect-1.4.10.jar, kotlin-script-runtime-1.4.0.jar, kotlin-script-runtime-1.4.10.jar,
  kotlin-scripting-common-1.4.10.jar, kotlin-scripting-compiler-embeddable-1.4.10.jar,
  kotlin-scripting-compiler-impl-embeddable-1.4.10.jar, kotlin-scripting-jvm-1.4.10.jar, kotlin-stdlib-1.4.0.jar,
  kotlin-stdlib-1.4.10.jar, kotlin-stdlib-common-1.4.0.jar, kotlin-stdlib-common-1.4.10.jar,
  kotlin-stdlib-jdk7-1.4.10.jar, kotlin-stdlib-jdk8-1.4.10.jar, kotlin-test-1.4.10.jar,
  kotlin-test-annotations-common-1.4.10.jar, kotlin-test-common-1.4.10.jar, kotlin-test-junit-1.4.10.jar
    * CVE-2020-29582
        * Korjattu 1.4.21
        * Lokaali hyökkääjä voi lukea tiedostoja ja hakemistoja koska niillä on väärät oikeudet.
* spring-security-data-5.2.4.RELEASE.jar, spring-security-test-5.2.4.RELEASE.jar
    * CVE-2021-22112
        * Korjattu 5.2.9.RELEASE
        * Vaatii ohjelmointivirheen
* commons-beanutils-1.9.3.jar
    * CVE-2019-10086
        * Deserialisointihaavoittuvuus
        * Deserialisoitavaa dataa ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
* guava-27.0.1-jre.jar, guava-29.0-jre.jar
    * CVE-2020-8908
        * com.google.common.io.Files.createTempDir():ssä haavoittuvuus, metodi ei käytössä
* junit-4.12.jar
    * CVE-2020-15250
        * Vaikuttaa vain testien aikana ja jos testejä ajavalla koneella on ei-luotettuja käyttäjiä
        * Korjattu 4.13.1:ssä
* log4j-api-2.11.2.jar, log4j-api-2.12.1.jar, log4j-to-slf4j-2.12.1.jar
    * CVE-2020-9488
        * Sertifikaattia ei validoida SMTP-appenderia käytettäessä. Ei käytetä SMTP-appenderia.
* postgresql-42.2.12.jar
    * CVE-2020-13692
        * XXE
        * XML:ää ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
        * Korjattu 42.2.13
* resteasy-client-3.9.1.Final.jar, resteasy-jackson2-provider-3.9.1.Final.jar, resteasy-jaxb-provider-3.9.1.Final.jar,
  resteasy-jaxrs-3.9.1.Final.jar, resteasy-multipart-provider-3.9.1.Final.jar
    * CVE-2021-10688
        * TODO
    * CVE-2020-1695
        * Korjattu 3.12.0.Final
        * Hankalahko hyväksikäyttää
    * CVE-2020-25633
        * Korjattu 4.5.6.Final:issa.
        * Hankalahko hyväksikäyttää
    * CVE-2021-20289
        * TODO
        * Korjattu 4.6.0.Final
    * CVE-2021-20293
        * TODO
        * Korjattu 4.6.0.Final
* snakeyaml-1.23.jar
    * CVE-2017-18640
        * DTD-entity expansion
        * Käytetäänkö ulkoisia DTD:itä ei-luotetuista lähteistä?
* spring-aop-5.1.5.RELEASE.jar, spring-core-5.1.5.RELEASE.jar
    * CVE-2020-5398
        * Vaatisi erikoisia ohjelmointivalintoja, erittäin epätodennäköinen
        * Korjattu 5.1.13
    * CVE-2020-5421
        * Korjattu 5.1.17
* spring-cloud-cloudfoundry-connector-2.0.7.RELEASE.jar (shaded: com.fasterxml.jackson.core:jackson-databind:2.10.0)
    * CVE-2020-25649
        * XXE
        * XML:ää ei vastaanoteta käyttäjiltä tai muilta ei-luotetuilta tahoilta
* commons-compress-1.20.jar
    * CVE-2021-35515, CVE-2021-35516, CVE-2021-35517, CVE-2021-36090
        * DoS
        * Vaatii käyttäjän syöttämän 7Z-, ZIP- tai tar-paketin käsittelyn. Ei käsitellä, joten ei ongelmaa.
* commons-io-2.6.jar
    * CVE-2021-29425
        * Haavoittuva metodi (FileNameUtils.normalize) ei käytössä.
* hibernate-validator-6.0.19.Final.jar
    * CVE-2020-10693
        * Korjattu 6.1.2.Final
        * Vaatisi erikoisia ohjelmointivalintoja, tuskin osuu meihin
* xmlsec-2.0.10.jar
    * CVE-2019-12400
        * Todella vaikea hyödyntää
        * Korjattu 2.1.4
* xnio-api-3.8.0.Final.jar, xnio-nio-3.8.0.Final.jar
    * CVE-2020-14340
        * DoS
        * Melko vaikea hyödyntää
* velocity-1.7.jar
    * CVE-2020-13936
        * Vaatii hyökkääjälle mahdollisuuden muokata Velocity-templateja, vaikea hyödyntää
 
