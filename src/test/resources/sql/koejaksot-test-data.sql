insert into jhi_authority (name)
values ('ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('5a0e2f03-ade4-45cf-9cd6-e074b1c6f426', 'erikoistuva1', 'Erkki', 'Erikoistuva', 'erikoistuva1@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('77fb2d0b-80c4-443a-8ed5-0144ae4e37d3', 'erikoistuva2', 'Erik', 'Erikoistuva', 'erikoistuva2@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('eb745779-3a4b-42ce-919b-18db6ed256af', 'erikoistuva3', 'Eerika', 'Erikoistuva', 'erikoistuva3@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('92ec93eb-7ec9-4ff2-9031-16592b7195d5', 'erikoistuva4', 'Eeva', 'Erikoistuva', 'erikoistuva4@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('666c93eb-7ec9-4ff2-9031-16592b719666', 'erikoistuva5', 'Eveliina', 'Erikoistuva', 'erikoistuva5@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('777c93eb-7ec9-4ff2-9031-16592b719777', 'erikoistuva6', 'Erkka', 'Erikoistuva', 'erikoistuva6@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('888c93eb-7ec9-4ff2-9031-16592b719888', 'erikoistuva7', 'Eevi', 'Erikoistuva', 'erikoistuva7@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('999c93eb-7ec9-4ff2-9031-16592b719999', 'erikoistuva8', 'Eija', 'Erikoistuva', 'erikoistuva8@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('f7f90358-0e7e-4bfb-82f7-75f1fce7887e', 'kouluttaja@test.com', 'Kalle', 'Kouluttaja', 'kouluttaja@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('d7f727e4-85aa-4293-b65d-95c52b100ce6', 'vastuuhenkilo@test.com', 'Ville', 'Vastuuhenkilo', 'vastuuhenkilo@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);


insert into jhi_user_authority (user_id, authority_name)
values ('5a0e2f03-ade4-45cf-9cd6-e074b1c6f426', 'ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user_authority (user_id, authority_name)
values ('77fb2d0b-80c4-443a-8ed5-0144ae4e37d3', 'ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user_authority (user_id, authority_name)
values ('eb745779-3a4b-42ce-919b-18db6ed256af', 'ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user_authority (user_id, authority_name)
values ('92ec93eb-7ec9-4ff2-9031-16592b7195d5', 'ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user_authority (user_id, authority_name)
values ('666c93eb-7ec9-4ff2-9031-16592b719666', 'ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user_authority (user_id, authority_name)
values ('777c93eb-7ec9-4ff2-9031-16592b719777', 'ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user_authority (user_id, authority_name)
values ('888c93eb-7ec9-4ff2-9031-16592b719888', 'ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user_authority (user_id, authority_name)
values ('999c93eb-7ec9-4ff2-9031-16592b719999', 'ROLE_ERIKOISTUVA_LAAKARI');

insert into jhi_user_authority (user_id, authority_name)
values ('f7f90358-0e7e-4bfb-82f7-75f1fce7887e', 'ROLE_KOULUTTAJA');

insert into jhi_user_authority (user_id, authority_name)
values ('f7f90358-0e7e-4bfb-82f7-75f1fce7887e', 'ROLE_LAHIKOULUTTAJA');

insert into jhi_user_authority (user_id, authority_name)
values ('d7f727e4-85aa-4293-b65d-95c52b100ce6', 'ROLE_VASTUUHENKILO');


insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (5001, null, null, '5a0e2f03-ade4-45cf-9cd6-e074b1c6f426', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (5002, null, null, '77fb2d0b-80c4-443a-8ed5-0144ae4e37d3', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (5003, null, null, 'eb745779-3a4b-42ce-919b-18db6ed256af', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (5004, null, null, '92ec93eb-7ec9-4ff2-9031-16592b7195d5', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (5005, null, null, '666c93eb-7ec9-4ff2-9031-16592b719666', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (5006, null, null, '777c93eb-7ec9-4ff2-9031-16592b719777', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (5007, null, null, '888c93eb-7ec9-4ff2-9031-16592b719888', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (5008, null, null, '999c93eb-7ec9-4ff2-9031-16592b719999', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (6001, null, null, 'f7f90358-0e7e-4bfb-82f7-75f1fce7887e', null, null);

insert into kayttaja (id, profiilikuva, profiilikuva_content_type, user_id, yliopisto_id, nimike)
values (7001, null, null, 'd7f727e4-85aa-4293-b65d-95c52b100ce6', null, null);


insert into erikoistuva_laakari (id, puhelinnumero, opiskelijatunnus, opintojen_aloitusvuosi, kayttaja_id,
                                 erikoisala_id)
values (4000, 01234567, 1234567, 2010, 5001, 46);

insert into erikoistuva_laakari (id, puhelinnumero, opiskelijatunnus, opintojen_aloitusvuosi, kayttaja_id,
                                 erikoisala_id)
values (4001, 01234567, 1234567, 2010, 5002, 46);

insert into erikoistuva_laakari (id, puhelinnumero, opiskelijatunnus, opintojen_aloitusvuosi, kayttaja_id,
                                 erikoisala_id)
values (4002, 01234567, 1234567, 2010, 5003, 46);

insert into erikoistuva_laakari (id, puhelinnumero, opiskelijatunnus, opintojen_aloitusvuosi, kayttaja_id,
                                 erikoisala_id)
values (4003, 01234567, 1234567, 2010, 5004, 46);

insert into erikoistuva_laakari (id, puhelinnumero, opiskelijatunnus, opintojen_aloitusvuosi, kayttaja_id,
                                 erikoisala_id)
values (4004, 01234567, 1234567, 2010, 5005, 46);

insert into erikoistuva_laakari (id, puhelinnumero, opiskelijatunnus, opintojen_aloitusvuosi, kayttaja_id,
                                 erikoisala_id)
values (4005, 01234567, 1234567, 2010, 5006, 46);

insert into erikoistuva_laakari (id, puhelinnumero, opiskelijatunnus, opintojen_aloitusvuosi, kayttaja_id,
                                 erikoisala_id)
values (4006, 01234567, 1234567, 2010, 5007, 46);

insert into erikoistuva_laakari (id, puhelinnumero, opiskelijatunnus, opintojen_aloitusvuosi, kayttaja_id,
                                 erikoisala_id)
values (4007, 01234567, 1234567, 2010, 5008, 46);

insert into koejakson_koulutussopimus (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_opiskelijatunnus,
                                       erikoistuvan_syntymaaika, erikoistuvan_yliopisto, opintooikeuden_myontamispaiva,
                                       koejakson_alkamispaiva, erikoistuvan_puhelinnumero, erikoistuvan_sahkoposti,
                                       lahetetty, muokkauspaiva, vastuuhenkilo_id, vastuuhenkilon_nimi,
                                       vastuuhenkilon_nimike, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                       korjausehdotus)
values (1000, 4000, 'Erkki Erikoistuva', 1234567, '1970-03-01', 'Yliopisto 1', null, '2020-03-11 12:50:31.526020', 01234567, 'erikoistuva1@test.com',
        true, '2020-05-12 12:50:31.526020', 7001, 'Ville Vastuuhenkilo', 'Dekaani', true, '2020-06-02 12:50:31.526020', null);

insert into koejakson_koulutussopimus (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_opiskelijatunnus,
                                       erikoistuvan_syntymaaika, erikoistuvan_yliopisto, opintooikeuden_myontamispaiva,
                                       koejakson_alkamispaiva, erikoistuvan_puhelinnumero, erikoistuvan_sahkoposti,
                                       lahetetty, muokkauspaiva, vastuuhenkilo_id, vastuuhenkilon_nimi,
                                       vastuuhenkilon_nimike, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                       korjausehdotus)
values (1001, 4001, 'Erik Erikoistuva', 1234567, '1970-03-01', 'Yliopisto 1', null, '2020-03-11 12:50:31.526020', 01234567, 'erikoistuva2@test.com',
        true, '2020-05-17 12:50:31.526020', 7001, 'Ville Vastuuhenkilo', 'Dekaani', true, '2020-05-18 12:50:31.526020', null);

insert into koejakson_koulutussopimus (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_opiskelijatunnus,
                                       erikoistuvan_syntymaaika, erikoistuvan_yliopisto, opintooikeuden_myontamispaiva,
                                       koejakson_alkamispaiva, erikoistuvan_puhelinnumero, erikoistuvan_sahkoposti,
                                       lahetetty, muokkauspaiva, vastuuhenkilo_id, vastuuhenkilon_nimi,
                                       vastuuhenkilon_nimike, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                       korjausehdotus)
values (1002, 4002, 'Eerika Erikoistuva', 1234567, '1970-03-01', 'Yliopisto 1', null, '2020-03-11 12:50:31.526020', 01234567, 'erikoistuva3@test.com',
        true, '2020-06-04 12:50:31.526020', 7001, 'Ville Vastuuhenkilo', 'Dekaani', true, '2020-06-05 12:50:31.526020', null);

insert into koejakson_koulutussopimus (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_opiskelijatunnus,
                                       erikoistuvan_syntymaaika, erikoistuvan_yliopisto, opintooikeuden_myontamispaiva,
                                       koejakson_alkamispaiva, erikoistuvan_puhelinnumero, erikoistuvan_sahkoposti,
                                       lahetetty, muokkauspaiva, vastuuhenkilo_id, vastuuhenkilon_nimi,
                                       vastuuhenkilon_nimike, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                       korjausehdotus)
values (1003, 4003, 'Eeva Erikoistuva', 1234567, '1970-03-01', 'Yliopisto 1', null, '2020-03-11 12:50:31.526020', 01234567, 'erikoistuva5@test.com',
        true, '2020-10-08 12:50:31.526020', 7001, 'Ville Vastuuhenkilo', 'Dekaani', true, '2020-06-05 12:50:31.526020', null);

insert into koejakson_koulutussopimus (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_opiskelijatunnus,
                                       erikoistuvan_syntymaaika, erikoistuvan_yliopisto, opintooikeuden_myontamispaiva,
                                       koejakson_alkamispaiva, erikoistuvan_puhelinnumero, erikoistuvan_sahkoposti,
                                       lahetetty, muokkauspaiva, vastuuhenkilo_id, vastuuhenkilon_nimi,
                                       vastuuhenkilon_nimike, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                       korjausehdotus)
values (1004, 4004, 'Eveliina Erikoistuva', 1234567, '1970-03-01', 'Yliopisto 1', null, '2020-03-11 12:50:31.526020', 01234567, 'erikoistuva6@test.com',
        true, '2020-12-08 12:50:31.526020', 7001, 'Ville Vastuuhenkilo', 'Dekaani', true, '2020-06-05 12:50:31.526020', null);

insert into koejakson_koulutussopimus (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_opiskelijatunnus,
                                       erikoistuvan_syntymaaika, erikoistuvan_yliopisto, opintooikeuden_myontamispaiva,
                                       koejakson_alkamispaiva, erikoistuvan_puhelinnumero, erikoistuvan_sahkoposti,
                                       lahetetty, muokkauspaiva, vastuuhenkilo_id, vastuuhenkilon_nimi,
                                       vastuuhenkilon_nimike, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                       korjausehdotus)
values (1005, 4005, 'Erkka Erikoistuva', 1234567, '1970-03-01', 'Yliopisto 1', null, '2020-03-11 12:50:31.526020', 01234567, 'erikoistuva7@test.com',
        true, '2020-11-04 12:50:31.526020', 7001, 'Ville Vastuuhenkilo', 'Dekaani', true, '2020-06-05 12:50:31.526020', null);

insert into koejakson_koulutussopimus (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_opiskelijatunnus,
                                       erikoistuvan_syntymaaika, erikoistuvan_yliopisto, opintooikeuden_myontamispaiva,
                                       koejakson_alkamispaiva, erikoistuvan_puhelinnumero, erikoistuvan_sahkoposti,
                                       lahetetty, muokkauspaiva, vastuuhenkilo_id, vastuuhenkilon_nimi,
                                       vastuuhenkilon_nimike, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                       korjausehdotus)
values (1006, 4006, 'Eevi Erikoistuva', 1234567, '1970-03-01', 'Yliopisto 1', null, '2020-03-11 12:50:31.526020', 01234567, 'erikoistuva8@test.com',
        true, '2020-11-04 12:50:31.526020', 7001, 'Ville Vastuuhenkilo', 'Dekaani', false, null, null);

insert into koejakson_koulutussopimus (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_opiskelijatunnus,
                                       erikoistuvan_syntymaaika, erikoistuvan_yliopisto, opintooikeuden_myontamispaiva,
                                       koejakson_alkamispaiva, erikoistuvan_puhelinnumero, erikoistuvan_sahkoposti,
                                       lahetetty, muokkauspaiva, vastuuhenkilo_id, vastuuhenkilon_nimi,
                                       vastuuhenkilon_nimike, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                       korjausehdotus)
values (1007, 4007, 'Eija Erikoistuva', 1234567, '1970-03-01', 'Yliopisto 1', null, '2020-03-11 12:50:31.526020', 01234567, 'erikoistuva8@test.com',
        true, '2020-10-01 12:50:31.526020', 7001, 'Ville Vastuuhenkilo', 'Dekaani', true, '2020-10-05 12:50:31.526020', null);

insert into koulutussopimuksen_kouluttaja (id, kouluttaja_id, nimi, nimike, toimipaikka, lahiosoite, postitoimipaikka,
                                           puhelin, sahkoposti, sopimus_hyvaksytty, kuittausaika, koulutussopimus_id)
values (1000, 6001, 'Kalle Kouluttaja', '', '', '', '', '', '',true, '2020-06-05 12:50:31.526020', 1000);

insert into koulutussopimuksen_kouluttaja (id, kouluttaja_id, nimi, nimike, toimipaikka, lahiosoite, postitoimipaikka,
                                           puhelin, sahkoposti, sopimus_hyvaksytty, kuittausaika, koulutussopimus_id)
values (1001, 6001, 'Kalle Kouluttaja', '', '', '', '', '', '',true, '2020-06-05 12:50:31.526020', 1001);

insert into koulutussopimuksen_kouluttaja (id, kouluttaja_id, nimi, nimike, toimipaikka, lahiosoite, postitoimipaikka,
                                           puhelin, sahkoposti, sopimus_hyvaksytty, kuittausaika, koulutussopimus_id)
values (1002, 6001, 'Kalle Kouluttaja', '', '', '', '', '', '',true, '2020-06-05 12:50:31.526020', 1002);

insert into koulutussopimuksen_kouluttaja (id, kouluttaja_id, nimi, nimike, toimipaikka, lahiosoite, postitoimipaikka,
                                           puhelin, sahkoposti, sopimus_hyvaksytty, kuittausaika, koulutussopimus_id)
values (1003, 6001, 'Kalle Kouluttaja', '', '', '', '', '', '',true, '2020-06-05 12:50:31.526020', 1003);

insert into koulutussopimuksen_kouluttaja (id, kouluttaja_id, nimi, nimike, toimipaikka, lahiosoite, postitoimipaikka,
                                           puhelin, sahkoposti, sopimus_hyvaksytty, kuittausaika, koulutussopimus_id)
values (1004, 6001, 'Kalle Kouluttaja', '', '', '', '', '', '',true, '2020-06-05 12:50:31.526020', 1004);

insert into koulutussopimuksen_kouluttaja (id, kouluttaja_id, nimi, nimike, toimipaikka, lahiosoite, postitoimipaikka,
                                           puhelin, sahkoposti, sopimus_hyvaksytty, kuittausaika, koulutussopimus_id)
values (1005, 6001, 'Kalle Kouluttaja', '', '', '', '', '', '',true, '2020-06-05 12:50:31.526020', 1005);

insert into koulutussopimuksen_kouluttaja (id, kouluttaja_id, nimi, nimike, toimipaikka, lahiosoite, postitoimipaikka,
                                           puhelin, sahkoposti, sopimus_hyvaksytty, kuittausaika, koulutussopimus_id)
values (1006, 6001, 'Kalle Kouluttaja', '', '', '', '', '', '',true, '2020-06-05 12:50:31.526020', 1006);

insert into koulutussopimuksen_kouluttaja (id, kouluttaja_id, nimi, nimike, toimipaikka, lahiosoite, postitoimipaikka,
                                           puhelin, sahkoposti, sopimus_hyvaksytty, kuittausaika, koulutussopimus_id)
values (1007, 6001, 'Kalle Kouluttaja', '', '', '', '', '', '',true, '2020-06-05 12:50:31.526020', 1007);

insert into koejakson_aloituskeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                         erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, erikoistuvan_sahkoposti,
                                         koejakson_suorituspaikka, koejakson_toinen_suorituspaikka,
                                         koejakson_alkamispaiva, koejakson_paattymispaiva, suoritettu_kokoaikatyossa,
                                         tyotunnit_viikossa, lahikouluttaja_id, lahikouluttajan_nimi,
                                         lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                         lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                         koejakson_osaamistavoitteet, lahetetty, muokkauspaiva, korjausehdotus)
values (1010, 4000, 'Erkki Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', '', 'Meilahden sairaala', '', '2020-11-30 12:50:31.526020',
        '2021-05-30 12:50:31.526020', true, 40, 6001, 'Kalle Kouluttaja', true, '2021-06-01 12:50:31.526020', 6001, '', true, '2021-06-01 12:50:31.526020',
        '', true, '2020-09-30 12:50:31.526020', '');

insert into koejakson_aloituskeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                         erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, erikoistuvan_sahkoposti,
                                         koejakson_suorituspaikka, koejakson_toinen_suorituspaikka,
                                         koejakson_alkamispaiva, koejakson_paattymispaiva, suoritettu_kokoaikatyossa,
                                         tyotunnit_viikossa, lahikouluttaja_id, lahikouluttajan_nimi,
                                         lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                         lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                         koejakson_osaamistavoitteet, lahetetty, muokkauspaiva, korjausehdotus)
values (1011, 4001, 'Erik Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', '', 'Meilahden sairaala', '', '2020-09-30 12:50:31.526020',
        '2021-03-30 12:50:31.526020', true, 40, 6001, 'Kalle Kouluttaja', true, '2021-04-01 12:50:31.526020', 6001, '', true, '2021-04-01 12:50:31.526020',
        '', true, '2021-01-01 12:50:31.526020', '');

insert into koejakson_aloituskeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                         erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, erikoistuvan_sahkoposti,
                                         koejakson_suorituspaikka, koejakson_toinen_suorituspaikka,
                                         koejakson_alkamispaiva, koejakson_paattymispaiva, suoritettu_kokoaikatyossa,
                                         tyotunnit_viikossa, lahikouluttaja_id, lahikouluttajan_nimi,
                                         lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                         lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                         koejakson_osaamistavoitteet, lahetetty, muokkauspaiva, korjausehdotus)
values (1012, 4002, 'Eerika Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', '', 'Meilahden sairaala', '', '2020-11-30 12:50:31.526020',
        '2021-05-30 12:50:31.526020', true, 40, 6001, 'Kalle Kouluttaja', true, '2021-06-01 12:50:31.526020', 6001, '', true, '2021-06-01 12:50:31.526020',
        '', true, '2021-02-01 12:50:31.526020', '');

insert into koejakson_aloituskeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                         erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, erikoistuvan_sahkoposti,
                                         koejakson_suorituspaikka, koejakson_toinen_suorituspaikka,
                                         koejakson_alkamispaiva, koejakson_paattymispaiva, suoritettu_kokoaikatyossa,
                                         tyotunnit_viikossa, lahikouluttaja_id, lahikouluttajan_nimi,
                                         lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                         lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                         koejakson_osaamistavoitteet, lahetetty, muokkauspaiva, korjausehdotus)
values (1013, 4003, 'Eeva Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', '', 'Meilahden sairaala', '', '2020-11-30 12:50:31.526020',
        '2021-05-30 12:50:31.526020', true, 40, 6001, 'Kalle Kouluttaja', false, '2021-06-01 12:50:31.526020', 6001, '', false, '2021-06-01 12:50:31.526020',
        '', true, '2021-03-01 12:50:31.526020', '');

insert into koejakson_aloituskeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                         erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, erikoistuvan_sahkoposti,
                                         koejakson_suorituspaikka, koejakson_toinen_suorituspaikka,
                                         koejakson_alkamispaiva, koejakson_paattymispaiva, suoritettu_kokoaikatyossa,
                                         tyotunnit_viikossa, lahikouluttaja_id, lahikouluttajan_nimi,
                                         lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                         lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                         koejakson_osaamistavoitteet, lahetetty, muokkauspaiva, korjausehdotus)
values (1014, 4004, 'Eveliina Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', '', 'Meilahden sairaala', '', '2020-11-30 12:50:31.526020',
        '2021-05-30 12:50:31.526020', true, 40, 6001, 'Kalle Kouluttaja', true, '2021-06-01 12:50:31.526020', 6001, '', true, '2021-06-01 12:50:31.526020',
        '', true, '2021-04-01 12:50:31.526020', '');

insert into koejakson_aloituskeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                         erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, erikoistuvan_sahkoposti,
                                         koejakson_suorituspaikka, koejakson_toinen_suorituspaikka,
                                         koejakson_alkamispaiva, koejakson_paattymispaiva, suoritettu_kokoaikatyossa,
                                         tyotunnit_viikossa, lahikouluttaja_id, lahikouluttajan_nimi,
                                         lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                         lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                         koejakson_osaamistavoitteet, lahetetty, muokkauspaiva, korjausehdotus)
values (1015, 4005, 'Erkka Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', '', 'Meilahden sairaala', '', '2020-11-30 12:50:31.526020',
        '2021-05-30 12:50:31.526020', true, 40, 6001, 'Kalle Kouluttaja', true, '2021-06-01 12:50:31.526020', 6001, '', true, '2021-06-01 12:50:31.526020',
        '', true, '2021-03-05 12:50:31.526020', '');

insert into koejakson_aloituskeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                         erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, erikoistuvan_sahkoposti,
                                         koejakson_suorituspaikka, koejakson_toinen_suorituspaikka,
                                         koejakson_alkamispaiva, koejakson_paattymispaiva, suoritettu_kokoaikatyossa,
                                         tyotunnit_viikossa, lahikouluttaja_id, lahikouluttajan_nimi,
                                         lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                         lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                         koejakson_osaamistavoitteet, lahetetty, muokkauspaiva, korjausehdotus)
values (1016, 4007, 'Eija Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', '', 'Meilahden sairaala', '', '2020-11-30 12:50:31.526020',
        '2021-05-30 12:50:31.526020', true, 40, 6001, 'Kalle Kouluttaja', true, '2021-06-01 12:50:31.526020', 6001, '', true, '2021-06-01 12:50:31.526020',
        '', true, '2020-12-20 12:50:31.526020', '');

insert into koejakson_valiarviointi (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                     erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                     edistyminen_tavoitteiden_mukaista, vahvuudet, kehittamistoimenpiteet,
                                     lahikouluttaja_id, lahikouluttajan_nimi, lahikouluttaja_hyvaksynyt,
                                     lahikouluttajan_kuittausaika, lahiesimies_id, lahiesimiehen_nimi,
                                     lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika, muokkauspaiva, korjausehdotus,
                                     erikoistuva_allekirjoittanut)
values (1020, 4000, 'Erkki Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, '', '', 6001, '', true, '2021-03-30 12:50:31.526020',
        6001, '', true, '2021-03-30 12:50:31.526020', '2021-05-01 12:50:31.526020', '', true);

insert into koejakson_valiarviointi (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                     erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                     edistyminen_tavoitteiden_mukaista, vahvuudet, kehittamistoimenpiteet,
                                     lahikouluttaja_id, lahikouluttajan_nimi, lahikouluttaja_hyvaksynyt,
                                     lahikouluttajan_kuittausaika, lahiesimies_id, lahiesimiehen_nimi,
                                     lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika, muokkauspaiva, korjausehdotus,
                                     erikoistuva_allekirjoittanut)
values (1021, 4001, 'Erik Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, '', '', 6001, '', true, '2021-03-30 12:50:31.526020',
        6001, '', true, '2021-03-30 12:50:31.526020', '2021-03-06 12:50:31.526020', '', true);

insert into koejakson_valiarviointi (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                     erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                     edistyminen_tavoitteiden_mukaista, vahvuudet, kehittamistoimenpiteet,
                                     lahikouluttaja_id, lahikouluttajan_nimi, lahikouluttaja_hyvaksynyt,
                                     lahikouluttajan_kuittausaika, lahiesimies_id, lahiesimiehen_nimi,
                                     lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika, muokkauspaiva, korjausehdotus,
                                     erikoistuva_allekirjoittanut)
values (1022, 4002, 'Eerika Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, '', '', 6001, '', true, '2021-03-30 12:50:31.526020',
        6001, '', true, '2021-03-30 12:50:31.526020', '2021-03-22 12:50:31.526020', '', true);

insert into koejakson_valiarviointi (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                     erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                     edistyminen_tavoitteiden_mukaista, vahvuudet, kehittamistoimenpiteet,
                                     lahikouluttaja_id, lahikouluttajan_nimi, lahikouluttaja_hyvaksynyt,
                                     lahikouluttajan_kuittausaika, lahiesimies_id, lahiesimiehen_nimi,
                                     lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika, muokkauspaiva, korjausehdotus,
                                     erikoistuva_allekirjoittanut)
values (1023, 4004, 'Eveliina Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, '', '', 6001, '', true, '2021-03-30 12:50:31.526020',
        6001, '', true, '2021-06-03 12:50:31.526020', '2021-03-22 12:50:31.526020', '', true);

insert into koejakson_valiarviointi (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                     erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                     edistyminen_tavoitteiden_mukaista, vahvuudet, kehittamistoimenpiteet,
                                     lahikouluttaja_id, lahikouluttajan_nimi, lahikouluttaja_hyvaksynyt,
                                     lahikouluttajan_kuittausaika, lahiesimies_id, lahiesimiehen_nimi,
                                     lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika, muokkauspaiva, korjausehdotus,
                                     erikoistuva_allekirjoittanut)
values (1024, 4005, 'Erkka Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, '', '', 6001, '', true, '2021-03-30 12:50:31.526020',
        6001, '', false, '2021-06-03 12:50:31.526020', '2021-03-22 12:50:31.526020', '', false);

insert into koejakson_valiarviointi (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                     erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                     edistyminen_tavoitteiden_mukaista, vahvuudet, kehittamistoimenpiteet,
                                     lahikouluttaja_id, lahikouluttajan_nimi, lahikouluttaja_hyvaksynyt,
                                     lahikouluttajan_kuittausaika, lahiesimies_id, lahiesimiehen_nimi,
                                     lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika, muokkauspaiva, korjausehdotus,
                                     erikoistuva_allekirjoittanut)
values (1025, 4007, 'Eija Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, '', '', 6001, '', false, '2021-03-30 12:50:31.526020',
        6001, '', false, '2021-06-03 12:50:31.526020', '2021-03-22 12:50:31.526020', '', false);

insert into koejakson_kehittamistoimenpiteet (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                              erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                              kehittamistoimenpiteet_riittavat, lahikouluttaja_id, lahikouluttajan_nimi,
                                              lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                              lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                              muokkauspaiva, korjausehdotus, erikoistuva_allekirjoittanut)
values (1030, 4000, 'Erkki Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, 6001, '', true, '2021-03-30 12:50:31.526020',
        6001, '', true, '2021-03-30 12:50:31.526020', '2021-04-30 12:50:31.526020', '', true);

insert into koejakson_kehittamistoimenpiteet (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                              erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                              kehittamistoimenpiteet_riittavat, lahikouluttaja_id, lahikouluttajan_nimi,
                                              lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                              lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                              muokkauspaiva, korjausehdotus, erikoistuva_allekirjoittanut)
values (1031, 4001, 'Erik Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, 6001, '', true, '2021-03-30 12:50:31.526020',
        6001, '', true, '2021-03-30 12:50:31.526020', '2021-04-01 12:50:31.526020', '', true);

insert into koejakson_kehittamistoimenpiteet (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                              erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                              kehittamistoimenpiteet_riittavat, lahikouluttaja_id, lahikouluttajan_nimi,
                                              lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                              lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                              muokkauspaiva, korjausehdotus, erikoistuva_allekirjoittanut)
values (1032, 4002, 'Eerika Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', false, 6001, '', false, '2021-03-30 12:50:31.526020',
        6001, '', false, '2021-03-30 12:50:31.526020', '2021-05-11 12:50:31.526020', 'Korjausehdotus', false);

insert into koejakson_kehittamistoimenpiteet (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                              erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                              kehittamistoimenpiteet_riittavat, lahikouluttaja_id, lahikouluttajan_nimi,
                                              lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                              lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                              muokkauspaiva, korjausehdotus, erikoistuva_allekirjoittanut)
values (1033, 4004, 'Eveliina Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, 6001, '', true, '2021-03-30 12:50:31.526020',
        6001, '', true, '2021-03-30 12:50:31.526020', '2021-04-11 12:50:31.526020', 'Korjausehdotus', true);


insert into koejakson_loppukeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                       erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                       esitetaan_koejakson_hyvaksymista, lahikouluttaja_id, lahikouluttajan_nimi,
                                       lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                       lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                       muokkauspaiva, korjausehdotus, erikoistuva_allekirjoittanut)
values (1040, 4000, 'Erkki Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, 6001, '', true, '2021-04-30 12:50:31.526020',
        6001, '', true, '2021-03-30 12:50:31.526020', '2021-05-30 12:50:31.526020', '', true);

insert into koejakson_loppukeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                       erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                       esitetaan_koejakson_hyvaksymista, lahikouluttaja_id, lahikouluttajan_nimi,
                                       lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                       lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                       muokkauspaiva, korjausehdotus, erikoistuva_allekirjoittanut)
values (1041, 4001, 'Erik Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, 6001, '', true, '2021-04-30 12:50:31.526020',
        6001, '', true, null, '2021-05-30 12:50:31.526020', '', false);

insert into koejakson_loppukeskustelu (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                       erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto,
                                       esitetaan_koejakson_hyvaksymista, lahikouluttaja_id, lahikouluttajan_nimi,
                                       lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika, lahiesimies_id,
                                       lahiesimiehen_nimi, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
                                       muokkauspaiva, korjausehdotus, erikoistuva_allekirjoittanut)
values (1042, 4004, 'Erik Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', true, 6001, '', true, '2021-04-30 12:50:31.526020',
        6001, '', true, null, '2021-05-30 12:50:31.526020', '', true);


insert into koejakson_vastuuhenkilon_arvio (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                            erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, vastuuhenkilo_id,
                                            vastuuhenkilon_nimi, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                            muokkauspaiva, erikoistuva_allekirjoittanut)
values (1050, 4000, 'Erkki Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', 7001, '', false, null,  '2021-06-05 12:50:31.526020', false);

insert into koejakson_vastuuhenkilon_arvio (id, erikoistuva_laakari_id, erikoistuvan_nimi, erikoistuvan_erikoisala,
                                            erikoistuvan_opiskelijatunnus, erikoistuvan_yliopisto, vastuuhenkilo_id,
                                            vastuuhenkilon_nimi, vastuuhenkilo_hyvaksynyt, vastuuhenkilon_kuittausaika,
                                            muokkauspaiva, erikoistuva_allekirjoittanut)
values (1051, 4004, 'Eveliina Erikoistuva', 'Työterveyshuolto', 1234567, 'Yliopisto 1', 7001, '', true, null,  '2021-06-17 12:50:31.526020', true);
