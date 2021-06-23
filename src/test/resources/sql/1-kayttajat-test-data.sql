insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('5a0e2f03-ade4-45cf-9cd6-e074b1c6f426', 'erikoistuva1@test.com', 'Erkki', 'Erikoistuva', 'erikoistuva1@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('77fb2d0b-80c4-443a-8ed5-0144ae4e37d3', 'erikoistuva2@test.com', 'Erik', 'Erikoistuva', 'erikoistuva2@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('eb745779-3a4b-42ce-919b-18db6ed256af', 'erikoistuva3@test.com', 'Eerika', 'Erikoistuva', 'erikoistuva3@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('92ec93eb-7ec9-4ff2-9031-16592b7195d5', 'erikoistuva4@test.com', 'Eeva', 'Erikoistuva', 'erikoistuva4@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('666c93eb-7ec9-4ff2-9031-16592b719666', 'erikoistuva5@test.com', 'Eveliina', 'Erikoistuva', 'erikoistuva5@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('777c93eb-7ec9-4ff2-9031-16592b719777', 'erikoistuva6@test.com', 'Erkka', 'Erikoistuva', 'erikoistuva6@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('888c93eb-7ec9-4ff2-9031-16592b719888', 'erikoistuva7@test.com', 'Eevi', 'Erikoistuva', 'erikoistuva7@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('999c93eb-7ec9-4ff2-9031-16592b719999', 'erikoistuva8@test.com', 'Eija', 'Erikoistuva', 'erikoistuva8@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('f7f90358-0e7e-4bfb-82f7-75f1fce7887e', 'kouluttaja@test.com', 'Kalle', 'Kouluttaja', 'kouluttaja@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
        null, null, null, null);

insert into jhi_user (id, login, first_name, last_name, email, activated, lang_key, created_by, created_date,
                      last_modified_by, last_modified_date, hetu, init_vector)
values ('1d628e68-1365-41a0-a102-438ad139c9de', 'kouluttaja_esimies@test.com', 'Kaarlo', 'Kouluttaja', 'kouluttaja_esimies@test.com', true, 'fi', 'system', '2020-06-11 12:50:31.526020',
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
values ('1d628e68-1365-41a0-a102-438ad139c9de', 'ROLE_KOULUTTAJA');

insert into jhi_user_authority (user_id, authority_name)
values ('1d628e68-1365-41a0-a102-438ad139c9de', 'ROLE_LAHIKOULUTTAJA');

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
values (6002, null, null, '1d628e68-1365-41a0-a102-438ad139c9de', null, null);

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
