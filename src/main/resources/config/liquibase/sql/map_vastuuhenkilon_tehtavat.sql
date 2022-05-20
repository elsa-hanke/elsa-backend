select distinct max(id) as id, yliopisto_id, erikoisala_id
into temp tmp_vastuuhenkilot
from kayttaja_yliopisto_erikoisala
where kayttaja_id in (select k.id
                      from kayttaja k
                               left join jhi_user u on k.user_id = u.id
                      where u.id in (select ua.user_id
                                     from jhi_user_authority ua
                                     where ua.authority_name = 'ROLE_VASTUUHENKILO'))
group by yliopisto_id, erikoisala_id;

do
$$
    declare
        kayttajaYliopistoErikoisalaId bigint;
        erikoisalaId                  bigint;
    begin
        for kayttajaYliopistoErikoisalaId, erikoisalaId in select id, erikoisala_id from tmp_vastuuhenkilot
            loop
                insert into rel_kayttaja_yliopisto_erikoisala__vastuuhenkilon_tehtavatyyppi
                values (kayttajaYliopistoErikoisalaId, 1);

                --Yleislääketiede
                if (erikoisalaId = 50) then
                    insert into rel_kayttaja_yliopisto_erikoisala__vastuuhenkilon_tehtavatyyppi
                    values (kayttajaYliopistoErikoisalaId, 2);
                end if;

                insert into rel_kayttaja_yliopisto_erikoisala__vastuuhenkilon_tehtavatyyppi
                values (kayttajaYliopistoErikoisalaId, 3);
                insert into rel_kayttaja_yliopisto_erikoisala__vastuuhenkilon_tehtavatyyppi
                values (kayttajaYliopistoErikoisalaId, 4);
        end loop;
    end
$$;

drop table tmp_vastuuhenkilot;
