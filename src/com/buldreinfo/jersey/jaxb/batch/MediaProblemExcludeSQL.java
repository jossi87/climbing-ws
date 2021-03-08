package com.buldreinfo.jersey.jaxb.batch;

public class MediaProblemExcludeSQL {
/*
SELECT CONCAT('INSERT INTO media_problem_exclude (media_id, problem_id) VALUES (',ms.media_id,',',p.id,'); -- ',CONCAT(r.url,'/problem/',p.id),' - ',a.name,' - ',s.name,' - ',p.name) note
FROM region r, area a, sector s, problem p, media_sector ms, svg
WHERE r.id=a.region_id AND a.id=s.area_id AND s.id=p.sector_id AND s.id=ms.sector_id AND ms.media_id=svg.media_id
AND p.id NOT IN (SELECT problem_id FROM svg)
AND (ms.media_id, p.id) NOT IN (SELECT media_id, problem_id FROM media_problem_exclude)
GROUP BY ms.media_id, p.id, r.url, a.name, s.name, p.name
ORDER BY r.id, a.name, s.name, p.name, ms.media_id
 */
}
