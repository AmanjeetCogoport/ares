package com.cogoport.ares.api.migration.constants

import java.util.UUID

object MigrationConstants {
    val createdUpdatedBy: UUID? = UUID.fromString("2f5e5152-03f4-4ea8-a3db-a6eff388161b")
    val uploadedByConstant: String = "MIGRATED"
    val administrativeExpense = """('COGO0235','COGO0235','COGO0468','COGO0468','COGO0468','COGO0514','COGO0542','COGO0542','COGO0542','COGO0772','COGO0985','49943','V00080','V00080','V00080','V00080','V00080','V00080','V001876','V001885','V001898','V001898','V001937','V001937','V001937','V001973','V001973','V001973','V001974','V001974','V001984','V001984','V001985','V001985','V001985','V001985','V001986','V001986','V001986','V002054','V002055','V002055','V002055','V002056','V002057','V002057','V002057','V002057','V002058','V002058','V002059','V002059','V002060','V002064','V002065','V002068','V002069','V002072','V002072','V002074','V002075','V002093','V002093','V002094','V002094','V002101','V002102','V002102','V002102','V002102','V002103','V002104','V002108','V002108','V002108','V002108','V002108','V002108','V002108','V00212','V00212','V002157','V002157','V002158','V002158','V002166','V00296','V00296','V00372','V00376','V00376','V00444','V00444','V00481','V00481','V00569','V00569','V00581','V00581','V00653','V00653','V00653','V00858','V00858','V00858','V00858','V00888','V00901','V00901','V00901','V00958','V00958','V00958','V00959','V00959','V00959','V00959','V00959','V00959','V00965','COGO0235','COGO0235','COGO0514','COGO0514','COGO0514','COGO0542','COGO0772','COGO0772','COGO0772','COGO0823','COGO0823','35137','35137','35447','36313','63357','63357','63357','67968','68355','68513','68634','68688','68688','68688','V00012','V00012','V00012','V00012','V00012','V00012','V00024','V00024','V00024','V00024','V00024','V00024','V00071','V00090','V00090','V00090','V00090','V00131','V00182','V001852','V001915','V001915','V001915','V001970','V001970','V001970','V001970','V001970','V001973','V001973','V001973','V001973','V001973','V001976','V001976','V001976','V001976','V001980','V001980','V001983','V001983','V001984','V001984','V001984','V001984','V001985','V001985','V001985','V001985','V001985','V001985','V001986','V001986','V001986','V001986','V001986','V001986','V001986','V001986','V001986','V002054','V002055','V002055','V002055','V002055','V002055','V002056','V002056','V002056','V002056','V002056','V002057','V002057','V002057','V002057','V002058','V002058','V002058','V002058','V002058','V002058','V002058','V002058','V002059','V002059','V002059','V002059','V002059','V002059','V002059','V002059','V002060','V002066','V002066','V002069','V002072','V002072','V002072','V002072','V002074','V002074','V002074','V002074','V002074','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002093','V002093','V002093','V002093','V002094','V002094','V002094','V002094','V002094','V002101','V002101','V002101','V002101','V002101','V002101','V002101','V002105','V002105','V002107','V002107','V002107','V002107','V002107','V002108','V002108','V002108','V002108','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002109','V002115','V002115','V002115','V002115','V002116','V002117','V002118','V002120','V002121','V002121','V002121','V002121','V002122','V002122','V002123','V002123','V002125','V002125','V002126','V002127','V002127','V002127','V002127','V002128','V002129','V002130','V002130','V002131','V002131','V002131','V002131','V002132','V002132','V002134','V002134','V002134','V002134','V002135','V002135','V002135','V002135','V002136','V002136','V002136','V002136','V002137','V002138','V002138','V002139','V002139','V002139','V002139','V002139','V002139','V002139','V002140','V002140','V002141','V002141','V002141','V002141','V002142','V002143','V002143','V002144','V002145','V002146','V002146','V002146','V002146','V002146','V002147','V002147','V002147','V002147','V002148','V002148','V002148','V002149','V002149','V002149','V002149','V002149','V002149','V002151','V002151','V002152','V002152','V002153','V002153','V002156','V002157','V002157','V002157','V002157','V002157','V002157','V002158','V002158','V002158','V002158','V002158','V002159','V002159','V002159','V002159','V002160','V002160','V002160','V002161','V002162','V002164','V002164','V002165','V002165','V002170','V002170','V002178','V002180','V002187','V002187','V002189','V002189','V002190','V002191','V002191','V002227','V002227','V002245','V002245','V00227','V00227','V00296','V00296','V00296','V00296','V00296','V00296','V00396','V00396','V00411','V00411','V00411','V00411','V00411','V00411','V00411','V00411','V00489','V00489','V00489','V00489','V00489','V00489','V00489','V00489','V00563','V00563','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00569','V00663','V00663','V00677','V00677','V00677','V00677','V00677','V00807','V00807','V00858','V00858','V00858','V00858','V00870','V00870','V00870','V00870','V00901','V00901','V00901','V00930','V00930','V00930','V00930','V00930','V00930','V00958','V00958','V00958','V00958','V00959','V00959','V00959','V00959','V00959','V00959','V00959','V00959','V00990','V00990','COGO-0597','COGO-0597','COGO1636','COGO1677','DHARMJEETSINGH','KRS001','COGO-0597','COGO0235','COGO0235','COGO0542','COGO0542','COGO0755','COGO0755','KRS001','0075M','0075M','35137','35445','60513','60513','63389','63389','63389','63389','63389','63389','TP49780','V00012','V00012','V00071','V00090','V00090','V00090','V00090','V00090','V00090','V00125','V00125','V00125','V00125','V00147','V00182','V00182','V00182','V001852','V001852','V001852','V001913','V001913','V001913','V001915','V001915','V001916','V001939','V001939','V001939','V001939','V001970','V001970','V001970','V001970','V001970','V001970','V001973','V001973','V001973','V001973','V001973','V001973','V001975','V001976','V001976','V001976','V001976','V001976','V001976','V001983','V001983','V001984','V001985','V002054','V002055','V002055','V002055','V002055','V002056','V002056','V002056','V002056','V002056','V002057','V002057','V002057','V002057','V002057','V002057','V002058','V002058','V002058','V002058','V002058','V002058','V002059','V002059','V002059','V002059','V002060','V002064','V002064','V002066','V002069','V002069','V002069','V002069','V002069','V002069','V002069','V002069','V002069','V002069','V002072','V002072','V002074','V002074','V002074','V002074','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002075','V002093','V002093','V002093','V002093','V002094','V002094','V002094','V002094','V002101','V002101','V002102','V002102','V002102','V002102','V002105','V002105','V002105','V002107','V002107','V002108','V002108','V002108','V002108','V002115','V002115','V002126','V002133','V002133','V002134','V002134','V002134','V002134','V002134','V002134','V002135','V002135','V002135','V002135','V002135','V002135','V002136','V002136','V002136','V002136','V002136','V002136','V002139','V002140','V002141','V002142','V002149','V002149','V002152','V002152','V002152','V002152','V002152','V002153','V002155','V002155','V002155','V002155','V002156','V002156','V002156','V002156','V002157','V002157','V002157','V002157','V002157','V002157','V002158','V002158','V002158','V002158','V002158','V002158','V002159','V002159','V002159','V002159','V002159','V002159','V002162','V002162','V002162','V002163','V002163','V002163','V002163','V002164','V002167','V002167','V002167','V002167','V002169','V002169','V002170','V002171','V002171','V002171','V002171','V002171','V002172','V002172','V002173','V002177','V002180','V002183','V002183','V002184','V002189','V002190','V002191','V002193','V002194','V002196','V002196','V002196','V002196','V002199','V002199','V002206','V002226','V002226','V002228','V002228','V002245','V002255','V002268','V002268','V002268','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V00227','V002299','V002316','V002319','V002323','V002324','V002325','V002325','V002326','V002328','V002330','V002331','V002331','V002331','V002331','V002336','V002338','V002342','V002342','V002342','V002342','V002342','V002344','V002346','V002346','V002346','V002347','V002350','V002350','V002350','V002351','V002352','V002355','V002361','V002367','V00252','V00252','V00252','V00252','V00296','V00296','V00296','V00296','V00296','V00296','V00344','V00348','V00411','V00411','V00411','V00411','V00411','V00411','V00411','V00411','V00411','V00489','V00489','V00489','V00489','V00489','V00489','V00489','V00489','V00563','V00563','V00568','V00568','V00621','V00622','V00663','V00663','V00677','V00677','V00677','V00677','V00840','V00860','V00860','V00860','V00870','V00870','V00871','V00901','V00901','V00901','V00901','V01000','V01000','19704','V00115','V001925','V001934','V001950','V001951','V002011','V002013','V002077','V002185','V002249','V002334','V00503','V00505','V00640','V00642','V00778','V00805','V00820','V00829','V00005','V00066','V00101','V001961','V002012','V002052','V002067','V002076','V002174','V002186','V002192','V002198','V002254','V002340','V00255','V00389','V00490','V00511','V00652','V00740','V00821','V00890','23658','COGO-0811','V00154','V001878','V00192','V00194','V001977','V001978','V00214','V00582','V00811','V00960','V00961','V00993','V00994','28693','V001897','V001900','V001901','V001933','V001949','V002015','V002016','V00223','V002256','V00254','V00345','V00419','V00447','V00461','V00508','V00509','V00570','V00620','V00647','V00749','V00894','V00916','V00910','V00916','V00915','V00926','V00927','V00928','COGO0579','COGO0483','V002312','V002308','V002309','V00818','V00819','V00893','SANDEEPKUMAR','V00866','V00872','V00999','V00954','V001872','V002124','V002247','V002248','V002311','V002332','V002273','COGO1523','COGO0826','COGO1000','COGO-0498','KRS003','COGO1421','COGO-0534','COGO-0821','COGO1447','COGO0102','COGO0158','COGO0512','E0000085','E0000097','E0000099','E0000249','E0000964','E0001100','E0001335','E0001747','V001922','V00307','V00806','COGO-0515','COGO0001','E0000707','E0001104','E0001242','E0001296','E0001449','E001390','V00063','V001917','V00248','V00422','0229M','0232M','279','COGO-1425','COGO0157','COGO0546','COGO0590','E0000173','E0000208','E0000222','E0000389','E0001389','E0001849','V00006','V00093','V00185','V00443','V00685','V00741','V00891','V00983','COGO-0506','COGO-0605','COGO-0793','COGO0180','COGO0924','E0001628','E0001646','MM01','TMS1','TMS2','V002071','V00739','V00838','0781M ','13 ','22 ','35340 ','35498 ','36046 ','40440','V002195','V002197','V002203','V002195','V002345','V002154','V002200','V002110','V002181','V002188','V002204','V002205','V002229','V002314','V002368','V002375','V002376','V002363','V002365','V002372','V002374','V002379','V002382','V002376','V002377','V002378','V002380','V002348','V002364','V002366','V002373','V00443','V002362','V002370','V002371')"""
}
