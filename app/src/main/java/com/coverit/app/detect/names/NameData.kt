package com.coverit.app.detect.names

import com.coverit.app.detect.text.Fold

object NameData {

    private fun set(raw: String) = raw.trim().split(Regex("\\s+")).map { Fold.text(it) }.toHashSet()

    val given = set(
        """
        antonio jose manuel francisco david juan javier daniel carlos jesus alejandro miguel rafael pedro angel pablo
        sergio fernando jorge luis alberto alvaro diego adrian raul ivan ruben enrique oscar andres ramon vicente joaquin
        santiago victor eduardo mario roberto jaime marcos ignacio alfonso hugo jordi salvador ricardo emilio guillermo
        gabriel julian julio tomas agustin marc martin gonzalo felix nicolas ismael cristian samuel joan aitor mariano
        domingo hector sebastian alfredo cesar felipe gregorio rodrigo iker lucas mateo leo alex unai pau oriol xavier
        gerard albert arnau pol eric aleix bernat jon mikel asier gorka inaki xabier ander ekaitz borja inigo nacho
        lorenzo esteban benito marcelino gustavo leandro rodolfo teodoro valentin bruno thiago dario izan marco nil
        aaron eloy ezequiel fermin gaspar german gines ginés hilario isidro jacinto leonardo lisandro mauricio moises
        nestor octavio pascual patricio rogelio saul simon urbano vidal anselmo amador apolinar arturo bartolome
        celestino ceferino cipriano clemente cristobal damian desiderio efren elias evaristo fabian faustino florencio
        maria carmen josefa isabel ana laura cristina marta dolores lucia francisca antonia elena sara paula mercedes
        raquel manuela rosario juana teresa beatriz pilar encarnacion julia silvia nuria irene alba patricia montserrat
        andrea rocio monica rosa alicia angela sonia sandra marina susana yolanda margarita natalia eva inmaculada claudia
        noelia carla esther veronica sofia angeles nerea amparo carolina inés ines miriam lorena victoria concepcion eva
        alejandra daniela emilia valeria martina jimena olga gloria matilde catalina ainhoa leire maite itziar amaia
        idoia nekane arantxa garazi uxue edurne miren begona agurtzane nahia irati june maialen ane laia marti
        meritxell neus montse nuria nuri clara judith ariadna aina berta mireia gemma anna nadia alma lola vega
        celia adriana aitana abril mia chloe olivia triana lara ainara ariana valentina candela blanca luna noa
        africa ascension asuncion aurora consuelo esperanza estrella eugenia fatima felisa fernanda guadalupe josefina
        leonor lourdes luisa macarena milagros nieves remedios rosalia soledad trinidad virginia ximena yaiza zaira
        agustina alicia amelia ana belen begoña carmela cecilia diana elisa elvira estefania eulalia fabiola filomena
        gabriela graciela hortensia iratxe ivonne jacinta joana lidia loreto lucrecia mar marisa marisol maribel
        mariana mariola marta minerva nazaret noemi paloma petra purificacion rebeca regina rita ruth sheila tamara
        vanesa vanessa veronica yesica jessica tania anabel
        mohamed mohammed muhammad ahmed ahmad hassan hussein ali omar youssef yusuf ibrahim mustafa abdel abdul
        karim rachid said hamid khalid mehdi bilal ayoub amine hamza ismail fatima fatma khadija aisha aicha amina
        meryem maryam salma nora zahra yasmin yasmina samira naima hanan latifa rachida
        ion ioan vasile gheorghe constantin mihai andrei alexandru florin adrian ionut cristina mihaela ioana
        elena andreea gabriela daniela alina roxana
        john james robert michael william david richard joseph thomas charles christopher daniel matthew anthony
        mark donald steven paul andrew joshua kenneth kevin brian george timothy ronald edward jason jeffrey ryan
        jacob gary nicholas eric jonathan stephen larry justin scott brandon benjamin samuel gregory alexander frank
        patrick raymond jack dennis jerry tyler aaron henry peter adam nathan zachary douglas harold kyle noah ethan
        oliver harry jacob charlie liam mason logan elijah owen
        mary patricia jennifer linda elizabeth barbara susan jessica sarah karen lisa nancy betty margaret sandra
        ashley kimberly emily donna michelle carol amanda dorothy melissa deborah stephanie rebecca sharon laura
        cynthia kathleen amy angela shirley anna brenda pamela emma nicole helen samantha katherine christine debra
        rachel carolyn janet catherine heather diane ruth julie olivia joyce virginia victoria kelly lauren christina
        joan evelyn judith megan andrea cheryl hannah jacqueline martha gloria teresa ann sara madison frances
        kathryn janice jean abigail alice judy sophia grace denise amber doris marilyn danielle beverly isabella
        theresa diana natalie brittany charlotte marie kayla alexis lori ava mia amelia
        jean pierre michel philippe alain nicolas francois jacques bernard christophe marie nathalie isabelle
        sylvie catherine francoise monique giuseppe giovanni francesco antonio mario luigi alessandro marco
        andrea stefano roberto paolo giulia francesca chiara federica valentina alessandra hans klaus jurgen
        wolfgang stefan andreas thomas michael heike sabine ursula joao jose antonio francisco paulo pedro
        luiz carlos maria ana francisca antonia adriana juliana marcia fernanda
        """,
    )

    val surnames = set(
        """
        garcia rodriguez gonzalez fernandez lopez martinez sanchez perez gomez martin jimenez ruiz hernandez diaz
        moreno munoz alvarez romero alonso gutierrez navarro torres dominguez vazquez ramos gil ramirez serrano blanco
        molina morales suarez ortega delgado castro ortiz rubio marin sanz nunez iglesias medina garrido cortes castillo
        santos lozano guerrero cano prieto mendez cruz calvo gallego herrera marquez leon vidal pena flores cabrera
        campos vega fuentes carrasco diez caballero reyes nieto aguilar pascual santana herrero montero lorenzo hidalgo
        gimenez ibanez ferrer duran santiago benitez vicente mora arias vargas carmona crespo roman pastor soto saez
        velasco moya soler parra esteban bravo gallardo rojas pardo merino franco espinosa izquierdo lara rivas silva
        rivera casado arroyo redondo camacho rey vera otero luque galan montes rios sierra segura carrillo marcos
        marti soriano mendoza robles bernal vila palacios exposito varela beltran bueno villar contreras pereira
        guerra andres mateo bermudez hurtado aguado salazar trujillo ferrero calderon gracia rosa escobar abad jurado
        pacheco miranda aranda manzano rincon salas toledo sola polo mesa barrera cuesta quintana avila rosales
        figueroa ponce lucas sosa ruano blazquez valero heredia sevilla alarcon morillo maldonado chacon nieves
        cuevas vergara rueda aparicio bautista cabello ballesteros benito sancho cordero mateos osorio pineda
        tomas mejias acosta zamora andreu peinado arenas llorente salinas gallego vallejo ferreira murillo
        quintero castellano rico valencia solis estevez paredes cuenca villa ojeda echeverria etxeberria
        aguirre agirre goikoetxea arrieta zubiri urrutia elorza larranaga ibarra azcona olano mugica garmendia
        puig serra roca pujol vives font riera costa sala mas vidal ribas bosch ferrer prat oliver coll pons
        casals soler bonet sabate llorens castells camps codina
        smith johnson williams brown jones miller davis wilson anderson taylor thomas moore jackson white harris
        thompson clark lewis robinson walker young allen king wright scott hill green adams baker nelson carter
        mitchell roberts turner phillips campbell parker evans edwards collins stewart morris murphy cook rogers
        morgan cooper peterson bailey reed kelly howard cox ward richardson watson brooks wood james bennett gray
        hughes price sanders myers long ross foster
        el alaoui benali bennani el amrani chaoui idrissi tazi berrada
        popescu ionescu popa pop dumitru stan stoica gheorghe matei constantin
        rossi russo ferrari esposito bianchi romano colombo ricci marino greco bruno gallo conti
        muller schmidt schneider fischer weber meyer wagner becker schulz hoffmann
        silva santos ferreira pereira oliveira costa rodrigues martins sousa fernandes goncalves
        dubois durand lefebvre moreau laurent simon michel leroy roux
        """,
    )

    val heading = set(
        """
        curriculum vitae cv resume resumé hoja vida datos personales personal details perfil profile profesional
        professional experiencia experience laboral formacion educacion education academica habilidades skills
        competencias idiomas languages contacto contact sobre mi about me informe report carta cover letter
        presentacion motivacion objetivo objective referencias references resumen summary portfolio portafolio
        certificado certificate factura invoice presupuesto contrato acta solicitud application expediente
        senior junior lead jefe jefa responsable director directora manager ingeniero ingeniera engineer
        developer desarrollador desarrolladora programador programadora disenador disenadora designer analista
        analyst consultor consultora consultant tecnico tecnica technician enfermero enfermera nurse medico medica
        doctor abogado abogada lawyer profesor profesora teacher administrativo administrativa asistente assistant
        comercial dependiente camarero camarera cocinero cocinera auxiliar especialista specialist coordinador
        coordinadora frontend backend fullstack full stack software web data datos marketing ventas sales
        recursos humanos rrhh hr
        """,
    )

    val stop = set(
        """
        hospital clinica clinic centro center servicio unidad consulta consultorio planta area departamento
        calle avenida avda plaza paseo camino carretera ronda pasaje barrio poligono urbanizacion
        san santa santo sant sao saint
        universidad universitat colegio instituto escuela facultad
        ayuntamiento ajuntament diputacion consejeria conselleria junta generalitat gobierno govern ministerio
        comunidad provincia estado reino region
        banco banca caja seguros seguridad social tesoreria agencia tributaria hacienda juzgado tribunal
        fundacion asociacion empresa grupo sociedad compania company inc ltd sl sa slu
        madrid barcelona valencia sevilla zaragoza malaga murcia palma bilbao alicante cordoba valladolid vigo
        gijon granada coruna vitoria elche oviedo badajoz cartagena terrassa jerez sabadell mostoles santander
        pamplona almeria burgos albacete castellon getafe salamanca logrono huelva cadiz leon tarragona lleida
        jaen ourense girona lugo caceres melilla ceuta toledo segovia soria teruel huesca cuenca zamora palencia
        avila guadalajara ciudad real compostela henares
        espana spain andalucia cataluna catalunya galicia asturias cantabria navarra aragon extremadura castilla
        mancha canarias baleares rioja euskadi
        lunes martes miercoles jueves viernes sabado domingo
        enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre
        monday tuesday wednesday thursday friday saturday sunday
        january february march april may june july august september october november december
        """,
    )
}
