package com.example.data.repository

import com.example.data.local.BookmarkDao
import com.example.data.local.CourseDao
import com.example.data.local.DepartmentDao
import com.example.data.local.ResourceDao
import com.example.data.model.Bookmark
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ResourceRepository(
    private val departmentDao: DepartmentDao,
    private val courseDao: CourseDao,
    private val resourceDao: ResourceDao,
    private val bookmarkDao: BookmarkDao
) {
    val allDepartments: Flow<List<Department>> = departmentDao.getAllDepartments()
    val favoriteResources: Flow<List<Resource>> = resourceDao.getFavoriteResources()
    val offlineResources: Flow<List<Resource>> = resourceDao.getDownloadedResources()

    fun getCourses(deptId: String, year: Int, semester: Int): Flow<List<Course>> =
        courseDao.getCourses(deptId, year, semester)

    fun getResourcesForCourse(courseId: String): Flow<List<Resource>> =
        resourceDao.getResourcesForCourse(courseId)

    fun getResourceById(id: String): Flow<Resource?> =
        resourceDao.getResourceById(id)

    fun searchResources(query: String): Flow<List<Resource>> =
        resourceDao.searchResources(query)

    suspend fun getCourseName(courseId: String): String {
        return courseDao.getCourseById(courseId)?.name ?: "Unknown Course"
    }

    suspend fun toggleFavorite(resourceId: String, isFavoriteCurrent: Boolean) {
        resourceDao.updateFavoriteState(resourceId, !isFavoriteCurrent)
    }

    suspend fun updateLastReadPage(resourceId: String, page: Int) {
        resourceDao.updateLastReadPage(resourceId, page)
    }

    fun getBookmarksForResource(resourceId: String): Flow<List<Bookmark>> =
        bookmarkDao.getBookmarksForResource(resourceId)

    suspend fun addBookmark(resourceId: String, pageNumber: Int, note: String) {
        val bookmark = Bookmark(resourceId = resourceId, pageNumber = pageNumber, note = note)
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmarkId: Int) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }

    // Advanced: simulated high-fidelity offline downloads
    suspend fun simulateDownload(resourceId: String, onProgressUpdate: (Int) -> Unit) {
        // Step-by-step progress updates for a premium visual feedback loop
        for (p in 1..10) {
            val progress = p * 10
            delay(150)
            onProgressUpdate(progress)
        }
        val path = "/storage/emulated/0/Android/data/com.aistudio.studentpocket/files/$resourceId.pdf"
        resourceDao.updateDownloadState(resourceId, isDownloaded = true, progress = 100, path = path)
    }

    suspend fun purgeResource(resourceId: String) {
        resourceDao.updateDownloadState(resourceId, isDownloaded = false, progress = 0, path = null)
    }

    // Auto-populates DB with a rich collection of realistic courses and handouts
    suspend fun seedDatabase() {
        // Verify if database is empty by reading the first item array of departments
        val currentDepts = departmentDao.getAllDepartments().first()
        if (currentDepts.isEmpty()) {
            val depts = listOf(
                // 1) Arba Minch Water Technology Institute
                Department("AWTI-HW", "Hydraulic & Water Resources Eng.", "AWTI-HW", "Hydrological cycles, open channel flows, and water engineering structures.", 5, "Water Technology Institute"),
                Department("AWTI-WS", "Water Supply & Environmental Eng.", "AWTI-WS", "Water treatment processes, pipe networks, and sanitary engineering.", 5, "Water Technology Institute"),
                Department("AWTI-WI", "Water Resources & Irrigation Eng.", "AWTI-WI", "Agricultural irrigation networks, dam structures, and watershed plans.", 5, "Water Technology Institute"),
                Department("AWTI-MH", "Meteorology & Hydrology", "AWTI-MH", "Analyzing climate telemetry patterns, rainfall forecasting, and weather models.", 4, "Water Technology Institute"),

                // 2) Arba Minch Institute of Technology (AMIT)
                Department("AMIT-CE", "Civil Engineering", "AMIT-CE", "Building structural integrity, geotechnical foundations, and transportation networks.", 5, "Institute of Technology"),
                Department("AMIT-SE", "Surveying Engineering", "AMIT-SE", "Geospatial coordinates, GIS layouts, photogrammetry, and geodetic measurements.", 5, "Institute of Technology"),
                Department("AMIT-EE", "Electrical & Computer Eng.", "AMIT-EE", "Microelectronics, power circuits, and telecommunication hardware.", 5, "Institute of Technology"),
                Department("AMIT-ME", "Mechanical Engineering", "AMIT-ME", "Thermodynamic boundaries, CAD modeling, and manufacturing systems.", 5, "Institute of Technology"),
                Department("AMIT-AP", "Architecture & Urban Planning", "AMIT-AP", "Physical environments, landscape aesthetics, and sustainable urban design.", 5, "Institute of Technology"),
                Department("AMIT-CS", "Computer Science", "AMIT-CS", "Computation, algorithm complexity, theory, and artificial systems.", 4, "Institute of Technology"),
                Department("AMIT-SWE", "Software Engineering", "AMIT-SWE", "Solid engineering designs, code methodologies, patterns, and dynamic application cycles.", 4, "Institute of Technology"),
                Department("AMIT-IT", "Information Technology", "AMIT-IT", "Enterprise networks, database deployments, cloud architectures, and web portals.", 4, "Institute of Technology"),

                // 3) College of Natural and Computational Sciences
                Department("CNCS-BI", "Biology", "CNCS-Bio", "Environmental biotechnology, microbiology, plant ecology, and genetics.", 4, "Natural & Computational Sciences"),
                Department("CNCS-BL", "Biological Sci. Lab Technology", "CNCS-BSLT", "Biology laboratory management, tissue cultures, specimen archiving, and analysis.", 4, "Natural & Computational Sciences"),
                Department("CNCS-BT", "Biotechnology", "CNCS-Biot", "Genetic manipulation, bio-processing, industrial enzyme production, and food biotechnology.", 4, "Natural & Computational Sciences"),
                Department("CNCS-CH", "Chemistry", "CNCS-Chem", "Industrial organic synthesis, inorganic structures, and chemical analysis.", 4, "Natural & Computational Sciences"),
                Department("CNCS-IC", "Industrial Chemistry", "CNCS-IndC", "Chemical plant management, polymer processing, petroleum refining, and cosmetics.", 4, "Natural & Computational Sciences"),
                Department("CNCS-CL", "Chemical Sci. Lab Technology", "CNCS-CSLT", "Chemical reagent storage safety, instrumentation calibration, and chromatography.", 4, "Natural & Computational Sciences"),
                Department("CNCS-FC", "Forensic Chemistry & Toxicology", "CNCS-FCT", "Noxious compound isolation, trace material assaying, and legal crime scene science.", 4, "Natural & Computational Sciences"),
                Department("CNCS-PH", "Physics", "CNCS-Phys", "Solid-state mechanics, quantum computing fields, and electromagnetism.", 4, "Natural & Computational Sciences"),
                Department("CNCS-PL", "Physics Lab Technology", "CNCS-PLT", "Spectroscopy setups, laser alignment, thin-film deposition, and vacuum calibration.", 4, "Natural & Computational Sciences"),
                Department("CNCS-GL", "Geology", "CNCS-Geo", "Rock mechanics, mineral exploration, plate tectonics, and petrology.", 4, "Natural & Computational Sciences"),
                Department("CNCS-MA", "Mathematics", "CNCS-Math", "Abstract algebra, numerical methods, statistics, and modeling.", 4, "Natural & Computational Sciences"),
                Department("CNCS-ST", "Statistics", "CNCS-Stat", "Stochastic models, probability trends, and corporate statistical forecasts.", 4, "Natural & Computational Sciences"),
                Department("CNCS-SP", "Sport Science", "CNCS-Sport", "Athletic kinetics, clinical exercise therapy, and physical training systems.", 4, "Natural & Computational Sciences"),

                // 4) College of Medicine and Health Sciences
                Department("CMHS-MD", "Medicine (M.D.)", "CMHS-Med", "Rigorous medical and diagnostic healthcare training with clinical internship rounds.", 6, "Medicine & Health Sciences"),
                Department("CMHS-PH", "Public Health", "CMHS-PH", "Epidemiological safety and community health outcomes across the continent.", 4, "Medicine & Health Sciences"),
                Department("CMHS-EH", "Environmental Health", "CMHS-EH", "Water safety, toxicological hazards, waste management, and campus hygiene.", 4, "Medicine & Health Sciences"),
                Department("CMHS-HI", "Health Informatics", "CMHS-HI", "Medical data portals, patient records systems, and digital safety tools.", 4, "Medicine & Health Sciences"),
                Department("CMHS-RD", "Radiology", "CMHS-RD", "Imaging scanners, sonography systems, CT layouts, and radiation security.", 4, "Medicine & Health Sciences"),
                Department("CMHS-MW", "Midwifery", "CMHS-MW", "Providing safe labor support, parent guidelines, and infant care.", 4, "Medicine & Health Sciences"),
                Department("CMHS-AN", "Anesthesia", "CMHS-AN", "Chemical dosage regulation, autonomic monitoring, and surgical preparations.", 4, "Medicine & Health Sciences"),
                Department("CMHS-ML", "Medical Laboratory Science", "CMHS-ML", "Processing hematological cultures, bacterial assays, and clinical serum chemistry.", 4, "Medicine & Health Sciences"),
                Department("CMHS-NS", "Nursing", "CMHS-NS", "Immediate therapy, acute diagnostic care, and critical patient support.", 4, "Medicine & Health Sciences"),
                Department("CMHS-PY", "Pharmacy", "CMHS-Phar", "Fostering pharmacology, synthesis, drug dispensing safety, and therapeutic outcomes.", 5, "Medicine & Health Sciences"),

                // 5) College of Agricultural Sciences
                Department("COAS-AS", "Animal Science", "COAS-Anim", "Animal nutrition, breeding dynamics, and wildlife management.", 4, "Agricultural Sciences"),
                Department("COAS-AH", "Animal Health", "COAS-AHlt", "Veterinary diagnostics, animal pathology, pharmacology, and clinical treatment.", 4, "Agricultural Sciences"),
                Department("COAS-FW", "Fisheries & Wildlife Mgmt.", "COAS-FWM", "Aquatic ecology, fish breeding, forestry preservation, and habitat management.", 4, "Agricultural Sciences"),
                Department("COAS-PS", "Plant Science", "COAS-Plnt", "Plant physiology, crop pathology, plant breeding, and soil biotechnology.", 4, "Agricultural Sciences"),
                Department("COAS-HO", "Horticulture", "COAS-Hort", "Greenhouse cultivation, vegetable production, floriculture, and plant propagation.", 4, "Agricultural Sciences"),
                Department("COAS-FP", "Food Science & Post-Harvest", "COAS-FSP", "Food processing, quality assurance, storage engineering, and nutrition metrics.", 4, "Agricultural Sciences"),
                Department("COAS-RD", "Rural Development & Ext.", "COAS-RDE", "Agricultural extension, community facilitation, and rural project design.", 4, "Agricultural Sciences"),
                Department("COAS-AB", "Agribusiness Value Chain Mgmt", "COAS-ABM", "Supply chains, cooperative finance, farm operations, and market strategy.", 4, "Agricultural Sciences"),
                Department("COAS-AE", "Agricultural Economics", "COAS-AEco", "Macroeconomic policy, resources, development finance, and trade models.", 4, "Agricultural Sciences"),
                Department("COAS-NR", "Natural Resource Management", "COAS-NRM", "Preserving watershed ecology, climate adaptive agriculture, and soil biology.", 4, "Agricultural Sciences"),
                Department("COAS-FR", "Forestry", "COAS-Forest", "Silviculture, timber preservation, and agroforestry ecosystems.", 4, "Agricultural Sciences"),

                // 6) College of Business and Economics
                Department("COBE-AF", "Accounting & Finance", "COBE-AF", "Enterprise bookkeeping, professional audit skills, and tax frameworks.", 4, "Business & Economics"),
                Department("COBE-EC", "Economics", "COBE-Eco", "Macroeconomic policy, development finance, and trade models.", 4, "Business & Economics"),
                Department("COBE-MN", "Management", "COBE-Mgmt", "Leadership practices, organizational operations, and small-business models.", 4, "Business & Economics"),
                Department("COBE-TM", "Tourism Management", "COBE-Tour", "Destination branding, travel booking, local guiding, and cultural archiving.", 4, "Business & Economics"),
                Department("COBE-HM", "Hotel Management", "COBE-Hotl", "Hospitality operations, lodging services, and culinary safety.", 4, "Business & Economics"),

                // 7) College of Social Sciences and Humanities
                Department("CSSH-CE", "Civics & Ethical Studies", "CSSH-Civ", "Constitutional citizenship, active policy review, and ethical guidelines.", 4, "Social Sciences & Humanities"),
                Department("CSSH-EN", "English Language & Lit.", "CSSH-Eng", "Classical prose, academic writing structures, and linguistic phonology.", 4, "Social Sciences & Humanities"),
                Department("CSSH-GG", "Geography & Env. Studies", "CSSH-Geog", "Demographic mapping, GIS analysis, and environmental safety.", 4, "Social Sciences & Humanities"),
                Department("CSSH-AM", "Ethiopian Languages-Amharic", "CSSH-Amh", "Morphology, grammar structures, and classical Ethiopian literature.", 4, "Social Sciences & Humanities"),
                Department("CSSH-HS", "History & Heritage Mgmt.", "CSSH-Hist", "Archiving horn of Africa historical chronicles and medieval conservation techniques.", 4, "Social Sciences & Humanities"),
                Department("CSSH-SO", "Sociology", "CSSH-Soc", "Analyzing social relationships, development paradigms, and community structures.", 4, "Social Sciences & Humanities"),
                Department("CSSH-SA", "Social Anthropology", "CSSH-Anth", "Cultural ethnology, indigenous community institutions, and heritage study.", 4, "Social Sciences & Humanities"),
                Department("CSSH-GM", "Ethiopian Languages-Gamogna", "CSSH-Gam", "Gamogna linguistic preservation, morphology, grammar, and folklore.", 4, "Social Sciences & Humanities"),

                // 8) School of Law
                Department("SOL-LW", "Law (LL.B.)", "SOL-Law", "Legal jurisprudence, judicial procedures, and constitutional justice.", 5, "School of Law"),

                // 9) School of Pedagogy and Behavioral Studies
                Department("SPBS-PY", "Psychology", "SPBS-Psyc", "Childhood cognitive development, counseling models, and behavioral sciences.", 4, "Pedagogy & Behavioral Studies"),
                Department("SPBS-LL", "Life Long Learning & Comm. Dev.", "SPBS-Life", "Formulating non-formal adult teaching and regional development programs.", 4, "Pedagogy & Behavioral Studies"),
                Department("SPBS-SN", "Special Needs & Inclusive Ed.", "SPBS-SNEd", "Accessible learning practices, sign languages, and inclusive designs.", 4, "Pedagogy & Behavioral Studies"),
                Department("SPBS-PS", "Pedagogical Science", "SPBS-Ped", "Developing curriculum designs, educational metrics, and lecture frameworks.", 4, "Pedagogy & Behavioral Studies"),

                // 10) Sawla Campus
                Department("SC-EM", "Electromechanical Engineering", "SC-EMech", "Integrating microcontrollers, electrical routing, and automation design.", 5, "Sawla Campus"),
                Department("SC-AE", "Automotive Engineering", "SC-Auto", "Vehicle mechanics, combustion engines, transmission, and braking systems.", 5, "Sawla Campus"),
                Department("SC-FE", "Food Engineering", "SC-Food", "Food processing equipment design, nutrition safety, and production channels.", 5, "Sawla Campus"),
                Department("SC-CE", "Civil Engineering (Sawla)", "SC-Civ", "Designing building frames, concrete structures, and soil testing.", 5, "Sawla Campus"),
                Department("SC-LS", "Logistics & Supply Chain Mgmt.", "SC-Log", "Optimizing warehouse allocations, inventory flows, and transportation.", 4, "Sawla Campus"),
                Department("SC-FEc", "Finance & Dev. Economics", "SC-Fin", "Archiving public finance allocations, microfinance loans, and trade balances.", 4, "Sawla Campus"),
                Department("SC-CA", "Cooperative Accounting & Auditing", "SC-Coop", "Structuring cooperative bookkeeping, dividend accounts, and audit checklists.", 4, "Sawla Campus"),
                Department("SC-BA", "Business Admin & Info Systems", "SC-BAIS", "Formulating corporate dashboards, database designs, and office workflow layouts.", 4, "Sawla Campus"),
                Department("SC-MM", "Marketing Management", "SC-Mktg", "Structuring consumer market research, branding tools, and promotional networks.", 4, "Sawla Campus"),
                Department("SC-CO", "Communication & Media Studies", "SC-Comm", "Fostering journalism, radio production, editing practices, and media writing.", 4, "Sawla Campus"),
                Department("SC-PR", "Public Relations & Advert", "SC-PRAd", "Structuring institutional communications, press releases, and campaign prints.", 4, "Sawla Campus"),
                Department("SC-GO", "Ethiopian Languages-Gofigna", "SC-Gof", "Archiving Gofigna vocabulary, structures, phonetic rules, and traditions.", 4, "Sawla Campus"),

                // 11) Freshman Program
                Department("FRESH-NS", "Natural Science (Freshman)", "FRESH-NS", "Common first-year courses for natural science, technology, and health streams.", 1, "Freshman Program"),
                Department("FRESH-SS", "Social Science (Freshman)", "FRESH-SS", "Common first-year courses for social science, business, and humanities streams.", 1, "Freshman Program")
            )
            departmentDao.insertDepartments(depts)

            val courses = listOf(
                // Freshman Program - Natural Science Stream
                // Semester 1 (1st semester)
                Course("FRESH-NS-111", "Introduction to Computer Science", "CoSc 1101", "FRESH-NS", 1, 1),
                Course("FRESH-NS-112", "Calculus I for Computing", "Math 1011", "FRESH-NS", 1, 1),
                Course("FRESH-NS-113", "General Physics", "Phys 1011", "FRESH-NS", 1, 1),
                Course("FRESH-NS-114", "General Chemistry", "Chem 1011", "FRESH-NS", 1, 1),

                // Semester 3 (Other natural sciences)
                Course("FRESH-NS-ON1", "General Biology", "Biol 1011", "FRESH-NS", 1, 3),
                Course("FRESH-NS-ON2", "Introduction to Statistics", "Stat 1012", "FRESH-NS", 1, 3),

                // Freshman Program - Social Science Stream
                Course("FRESH-SS-111", "Introduction to Geography", "Geog 1011", "FRESH-SS", 1, 1),
                Course("FRESH-SS-112", "General Psychology", "Psyc 1011", "FRESH-SS", 1, 1),
                Course("FRESH-SS-121", "Introduction to Economics", "Econ 1011", "FRESH-SS", 1, 2),

                // School of Computing (AMIT-CS) - Starts from Year 2
                // (Populated accurately from the full curriculum below)

                // Electrical & Computer Eng. (AMIT-EE) - Starts from Year 2
                Course("AMIT-EE-211", "Basic Circuits Analysis", "ECE 1011", "AMIT-EE", 2, 1),
                Course("AMIT-EE-311", "Signals and Systems", "ECE 3111", "AMIT-EE", 3, 1),
                Course("AMIT-EE-511", "Wireless Communications", "ECE 5112", "AMIT-EE", 5, 1),

                // Civil Engineering (AMIT-CE) - Starts from Year 2
                Course("AMIT-CE-211", "Engineering Mechanics I", "CEng 1011", "AMIT-CE", 2, 1),
                Course("AMIT-CE-322", "Hydraulics & Water Engineering", "CEng 3012", "AMIT-CE", 3, 2),
                Course("AMIT-CE-522", "Hydropower Structure Design", "CEng 5102", "AMIT-CE", 5, 2),

                // School of Medicine (CMHS-MD) - Starts from Year 2
                Course("CMHS-MD-211", "Human Anatomy I", "Anat 1101", "CMHS-MD", 2, 1),
                Course("CMHS-MD-322", "Clinical Pathology", "Path 3102", "CMHS-MD", 3, 2),
                Course("CMHS-MD-622", "Advanced Internal Medicine", "IntM 6102", "CMHS-MD", 6, 2),

                // Accounting & Finance (COBE-AF) - Starts from Year 2
                Course("COBE-AF-211", "Introductory Accounting", "Acct 1101", "COBE-AF", 2, 1),
                Course("COBE-AF-222", "Financial Accounting II", "Acct 2112", "COBE-AF", 2, 2),
                Course("COBE-AF-411", "Auditing Principles 1", "Acct 4115", "COBE-AF", 4, 1),

                // School of Law (SOL-LW) - Starts from Year 2
                Course("SOL-LW-211", "Intro to Law & Legal History", "Law 1011", "SOL-LW", 2, 1),
                Course("SOL-LW-311", "Ethiopian Constitutional Law I", "Law 3121", "SOL-LW", 3, 1),
                Course("SOL-LW-511", "International Human Rights Law", "Law 5115", "SOL-LW", 5, 1),

                // Department of Plant Sciences (COAS-PS)
                Course("COAS-PS-211", "Principles of Agronomy", "PlSc 2111", "COAS-PS", 2, 1),
                Course("COAS-PS-322", "Soil Fertility & Plant Nutrition", "PlSc 3112", "COAS-PS", 3, 2),

                // Water Technology Institute (AWTI-HW)
                Course("AWTI-HW-111", "Fluid Mechanics I", "HWeng 2101", "AWTI-HW", 2, 1),

                // Natural Sciences (CNCS-BI)
                Course("CNCS-BI-111", "General Microbiology", "Bio 2111", "CNCS-BI", 2, 1),

                // Sawla Campus Electromechanical Engineering (SC-EM)
                Course("SC-EM-111", "Introduction to Mechatronics", "SC-EM 2101", "SC-EM", 2, 1),

                // NEW: Full Computer Science Curriculum
                // 1st Year, 1st Semester (Freshman Natural)
                Course("CS-Y1S1-01", "Mathematics for Natural Science", "Math-1011", "FRESH-NS", 1, 1),
                Course("CS-Y1S1-02", "Communicative English Language Skills I", "FLEn-1011", "FRESH-NS", 1, 1),
                Course("CS-Y1S1-03", "General Physics", "Phys-1011", "FRESH-NS", 1, 1),
                Course("CS-Y1S1-04", "General Psychology", "Psch-1011", "FRESH-NS", 1, 1),
                Course("CS-Y1S1-05", "Critical Thinking", "LoCT-1011", "FRESH-NS", 1, 1),
                Course("CS-Y1S1-06", "Physical Fitness", "SpSc-1011", "FRESH-NS", 1, 1),
                Course("CS-Y1S1-07", "Geography of Ethiopia and the Horn", "GeES-1011", "FRESH-NS", 1, 1),

                // 1st Year, 2nd Semester (Pre-Engineering)
                Course("CS-Y1S2-01", "Communicative English Language Skills II", "FLEn-1012", "FRESH-NS", 1, 2),
                Course("CS-Y1S2-02", "Social Anthropology", "Anth-1012", "FRESH-NS", 1, 2),
                Course("CS-Y1S2-03", "Applied Mathematics I", "Math-1041", "FRESH-NS", 1, 2),
                Course("CS-Y1S2-04", "Introduction to Emerging Technologies", "EmTe-1012", "FRESH-NS", 1, 2),
                Course("CS-Y1S2-05", "Moral and Civic Education", "MCiE-1012", "FRESH-NS", 1, 2),
                Course("CS-Y1S2-06", "Computer programming", "CoSc-1012", "FRESH-NS", 1, 2),
                Course("CS-Y1S2-07", "Economics", "Econ-1011", "FRESH-NS", 1, 2),
                Course("CS-Y1S2-08", "History of Ethiopia and the Horn", "Hist-1012", "FRESH-NS", 1, 2),
                Course("CS-Y1S2-09", "Entrepreneurship", "MGMT-1012", "FRESH-NS", 1, 2),

                // 2nd Year, 1st Semester
                Course("CS-Y2S1-01", "Digital Logic Design", "EENG-2041", "AMIT-CS", 2, 1),
                Course("CS-Y2S1-02", "Object Oriented Programming", "CoSc-2051", "AMIT-CS", 2, 1),
                Course("CS-Y2S1-03", "Linear Algebra", "MATH-2011", "AMIT-CS", 2, 1),
                Course("CS-Y2S1-04", "Fundamentals of Database Systems", "CoSc-2041", "AMIT-CS", 2, 1),
                Course("CS-Y2S1-05", "Probability and Statistics", "STAT-2015", "AMIT-CS", 2, 1),
                Course("CS-Y2S1-06", "Inclusiveness", "SINE-2011", "AMIT-CS", 2, 1),

                // 2nd Year, 2nd Semester
                Course("CS-Y2S2-01", "Data Communication and Computer Networks", "CoSc-2032", "AMIT-CS", 2, 2),
                Course("CS-Y2S2-02", "Advanced Database Systems", "CoSc-2042", "AMIT-CS", 2, 2),
                Course("CS-Y2S2-03", "Numerical Analysis", "MATH-2082", "AMIT-CS", 2, 2),
                Course("CS-Y2S2-04", "Discrete Mathematics and Combinatorics", "MATH-2052", "AMIT-CS", 2, 2),
                Course("CS-Y2S2-05", "Data Structures and Algorithms", "CoSc-2092", "AMIT-CS", 2, 2),
                Course("CS-Y2S2-06", "Computer organization and Architecture", "CoSc-2022", "AMIT-CS", 2, 2),

                // 3rd Year, 1st Semester
                Course("CS-Y3S1-01", "Operating Systems", "CoSc-3023", "AMIT-CS", 3, 1),
                Course("CS-Y3S1-02", "Web programming", "CoSc-3081", "AMIT-CS", 3, 1),
                Course("CS-Y3S1-03", "Java Programming", "CoSc-3053", "AMIT-CS", 3, 1),
                Course("CS-Y3S1-04", "Software Engineering", "CoSc-3061", "AMIT-CS", 3, 1),
                Course("CS-Y3S1-05", "Automata and Complexity Theory", "CoSc-3101", "AMIT-CS", 3, 1),
                Course("CS-Y3S1-06", "Microprocessor and Assembly Language Programming", "CoSc-3025", "AMIT-CS", 3, 1),
                Course("CS-Y3S1-07", "Global Trends", "IRGI-3021", "AMIT-CS", 3, 1),
                Course("CS-Y3S1-08", "System Analysis and Design", "ITec-3061", "AMIT-CS", 3, 1),
                Course("CS-Y3S1-09", "Multimedia Systems", "ITec-3121", "AMIT-CS", 3, 1),

                // 3rd Year, 2nd Semester
                Course("CS-Y3S2-01", "Wireless Communication and Mobile Computing", "CoSc-3034", "AMIT-CS", 3, 2),
                Course("CS-Y3S2-02", "Introduction to Artificial Intelligence", "CoSc-3112", "AMIT-CS", 3, 2),
                Course("CS-Y3S2-03", "Design and Analysis of Algorithms", "CoSc-3094", "AMIT-CS", 3, 2),
                Course("CS-Y3S2-04", "Real Time and Embedded Systems", "CoSc-3026", "AMIT-CS", 3, 2),
                Course("CS-Y3S2-05", "Computer Graphics", "CoSc-3072", "AMIT-CS", 3, 2),
                Course("CS-Y3S2-06", "Industrial Practice", "CoSc-3122", "AMIT-CS", 3, 2),

                // 4th Year, 1st Semester
                Course("CS-Y4S1-01", "Computer Security", "CoSc-4035", "AMIT-CS", 4, 1),
                Course("CS-Y4S1-02", "Computer Vision and Image Processing", "CoSc-4113", "AMIT-CS", 4, 1),
                Course("CS-Y4S1-03", "Research Methods in Computer Science", "CoSc-4123", "AMIT-CS", 4, 1),
                Course("CS-Y4S1-04", "Compiler Design", "CoSc-4103", "AMIT-CS", 4, 1),
                Course("CS-Y4S1-05", "Final Year Project I", "CoSc-4125", "AMIT-CS", 4, 1),
                Course("CS-Y4S1-06", "Multimedia (Elective Course)", "CoSc-4077", "AMIT-CS", 4, 1),

                // 4th Year, 2nd Semester
                Course("CS-Y4S2-01", "Network and System Administration", "CoSc-4036", "AMIT-CS", 4, 2),
                Course("CS-Y4S2-02", "Introduction to Distributed Systems", "CoSc-4038", "AMIT-CS", 4, 2),
                Course("CS-Y4S2-03", "Selected Topics in Computer Science", "CoSc-4132", "AMIT-CS", 4, 2),
                Course("CS-Y4S2-04", "Final Year Project II", "CoSc-4126", "AMIT-CS", 4, 2),
                Course("CS-Y4S2-05", "Introduction to Data Mining and Data Warehousing", "CoSc-4112", "AMIT-CS", 4, 2),
                Course("CS-Y4S2-06", "Introduction to Machine Learning (Elective II)", "CoSc-4114", "AMIT-CS", 4, 2),
                Course("CS-Y4S2-07", "National Exit Exam", "Exit-01", "AMIT-CS", 4, 2),

                // WRIE 2nd Year, 1st Semester
                Course("WRIE-Y2S1-01", "Engineering Mechanics", "CEng-2035", "AWTI-WI", 2, 1),
                Course("WRIE-Y2S1-02", "Technical Drawing", "MEng-2034", "AWTI-WI", 2, 1),
                Course("WRIE-Y2S1-03", "Construction Materials and Equipment", "CEng-2081", "AWTI-WI", 2, 1),
                Course("WRIE-Y2S1-04", "Engineering Geology", "Geol-2081", "AWTI-WI", 2, 1),
                Course("WRIE-Y2S1-05", "Surveying I", "CEng-2051", "AWTI-WI", 2, 1),
                Course("WRIE-Y2S1-06", "Probability and Statistics", "Stat-2091", "AWTI-WI", 2, 1),
                Course("WRIE-Y2S1-07", "Applied Mathematics II", "Math-2043", "AWTI-WI", 2, 1),

                // WRIE 2nd Year, 2nd Semester
                Course("WRIE-Y2S2-01", "Surveying II", "CEng-2052", "AWTI-WI", 2, 2),
                Course("WRIE-Y2S2-02", "Fluid Mechanics", "WRIE-2101", "AWTI-WI", 2, 2),
                Course("WRIE-Y2S2-03", "Numerical Analysis", "Math-2094", "AWTI-WI", 2, 2),
                Course("WRIE-Y2S2-04", "Introduction to Hydrology", "WRIE-2092", "AWTI-WI", 2, 2),
                Course("WRIE-Y2S2-05", "Strength of Materials", "CEng-2061", "AWTI-WI", 2, 2),
                Course("WRIE-Y2S2-06", "Soil Physics", "WRIE-2111", "AWTI-WI", 2, 2),
                Course("WRIE-Y2S2-07", "Hydrological Measurements and Analysis", "WRIE-2093", "AWTI-WI", 2, 2),
                Course("WRIE-Y2S2-08", "Building Construction", "CEng-2082", "AWTI-WI", 2, 2),

                // WRIE 3rd Year, 1st Semester
                Course("WRIE-Y3S1-01", "Groundwater Engineering", "WRIE-3096", "AWTI-WI", 3, 1),
                Course("WRIE-Y3S1-02", "Surface Irrigation", "WRIE-3112", "AWTI-WI", 3, 1),
                Course("WRIE-Y3S1-03", "Reinforced Concrete Design I", "CEng-3062", "AWTI-WI", 3, 1),
                Course("WRIE-Y3S1-04", "Hydraulics", "WRIE-3102", "AWTI-WI", 3, 1),
                Course("WRIE-Y3S1-05", "Engineering Hydrology", "WRIE-3095", "AWTI-WI", 3, 1),
                Course("WRIE-Y3S1-06", "Soil Mechanics I", "CEng-3072", "AWTI-WI", 3, 1),
                Course("WRIE-Y3S1-07", "Inclusiveness", "MCiE-3056", "AWTI-WI", 3, 1),
                Course("WRIE-Y3S1-08", "Economics", "WRIE-3204", "AWTI-WI", 3, 1),

                // WRIE 3rd Year, 2nd Semester
                Course("WRIE-Y3S2-01", "Irrigation Structures I", "WRIE-3114", "AWTI-WI", 3, 2),
                Course("WRIE-Y3S2-02", "Soil Mechanics II", "CEng-3073", "AWTI-WI", 3, 2),
                Course("WRIE-Y3S2-03", "Pump Design and Installation", "WRIE-3121", "AWTI-WI", 3, 2),
                Course("WRIE-Y3S2-04", "Reinforced Concrete Design II", "CEng-3063", "AWTI-WI", 3, 2),
                Course("WRIE-Y3S2-05", "Pressurized Irrigation", "WRIE-3113", "AWTI-WI", 3, 2),
                Course("WRIE-Y3S2-06", "Open Channel Hydraulics", "WRIE-3103", "AWTI-WI", 3, 2),
                Course("WRIE-Y3S2-07", "Dam Engineering-I", "WRIE-3131", "AWTI-WI", 3, 2),

                // WRIE 4th Year, 1st Semester
                Course("WRIE-Y4S1-01", "Foundation Engineering", "CEng-4074", "AWTI-WI", 4, 1),
                Course("WRIE-Y4S1-02", "Research Methods", "WRIE-4141", "AWTI-WI", 4, 1),
                Course("WRIE-Y4S1-03", "Drainage Engineering", "WRIE-4116", "AWTI-WI", 4, 1),
                Course("WRIE-Y4S1-04", "Water Supply and Sanitation Engineering", "WRIE-4122", "AWTI-WI", 4, 1),
                Course("WRIE-Y4S1-05", "Irrigation Structures II", "WRIE-4115", "AWTI-WI", 4, 1),
                Course("WRIE-Y4S1-06", "Software Application in WRIE", "WRIE-4161", "AWTI-WI", 4, 1),
                Course("WRIE-Y4S1-07", "Dam Engineering II", "WRIE-4132", "AWTI-WI", 4, 1),

                // WRIE 4th Year, 2nd Semester
                Course("WRIE-Y4S2-01", "Holistic Exam", "WRIE-4151", "AWTI-WI", 4, 2),
                Course("WRIE-Y4S2-02", "Internship Practice Company Evaluation", "WRIE-4152", "AWTI-WI", 4, 2),
                Course("WRIE-Y4S2-03", "Internship Practice Report Evaluation", "WRIE-4153", "AWTI-WI", 4, 2),
                Course("WRIE-Y4S2-04", "Internship Practice Presentation and Defense", "WRIE-4154", "AWTI-WI", 4, 2),

                // WRIE 5th Year, 1st Semester
                Course("WRIE-Y5S1-01", "Water Resources Planning & Management", "WRIE-5144", "AWTI-WI", 5, 1),
                Course("WRIE-Y5S1-02", "GIS & Remote Sensing", "WRIE-5143", "AWTI-WI", 5, 1),
                Course("WRIE-Y5S1-03", "Construction Planning and Management", "CEng-5172", "AWTI-WI", 5, 1),
                Course("WRIE-Y5S1-04", "Contract, Specification and Quantity Surveying", "CEng-5171", "AWTI-WI", 5, 1),
                Course("WRIE-Y5S1-05", "Engineering Economics", "CEng-5173", "AWTI-WI", 5, 1),
                Course("WRIE-Y5S1-06", "River Engineering & Sediment Transport", "WRIE-5133", "AWTI-WI", 5, 1),
                Course("WRIE-Y5S1-07", "Soil and Water Conservation Engineering", "WRIE-5142", "AWTI-WI", 5, 1),
                Course("WRIE-Y5S1-08", "Irrigation Water Management", "WRIE-5117", "AWTI-WI", 5, 1),

                // WRIE 5th Year, 2nd Semester
                Course("WRIE-Y5S2-01", "Global Trends", "WRIE-5162", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-02", "Final Year Project", "WRIE-5163", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-03", "Environmental Impact Assessment", "WRIE-5146", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-04", "Watershed Management and Modeling", "WRIE-5147", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-05", "Principles of Hydropower & Alternate Energy sources", "WRIE-5134", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-06", "Road Engineering", "CEng-5075", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-07", "Water Law and Hydro Politics", "WRIE-5145", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-08", "Educational Field Practice", "WRIE-5155", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-09", "History of Ethiopia and the Horn", "Hist-1023", "AWTI-WI", 5, 2),
                Course("WRIE-Y5S2-10", "National Exit Exam", "Exit-01", "AWTI-WI", 5, 2)
            )
            courseDao.insertCourses(courses)

            val resources = listOf(
                // Introduction to Computer Science (FRESH-NS-111)
                Resource("RES_CS111_01", "FRESH-NS-111", "History of Electronic Computing Devices Slides", "Lecture Notes", "1.8 MB", "A highly scannable visual history of mechanical calculating tools, Turing machines, vacuum tubes, and microprocessors."),
                Resource("RES_CS111_02", "FRESH-NS-111", "Communicative Computing Syllabus AMU 2026", "Handouts", "450 KB", "Official department guide with study calendars, required textbooks, and lab reporting grading weight rules."),
                Resource("RES_CS111_03", "FRESH-NS-111", "Computer Science Term Final Past Exam - 2024", "Past Exams", "1.2 MB", "Includes official answers for number system conversions, basic Boolean logic gates, and pseudo-code algorithm design."),

                // Calculus I for Computing (FRESH-NS-112)
                Resource("RES_CS112_01", "FRESH-NS-112", "Limits & Continuity Core Concepts", "Lecture Notes", "2.1 MB", "Comprehensive reference notes with step-by-step rigorous delta-epsilon proofs and limit calculations with infinity."),
                Resource("RES_CS112_02", "FRESH-NS-112", "Derivatives Assignment Worksheet", "Assignments", "890 KB", "Includes the complete problem set (25 advanced calculus questions) on the chain rule, implicit differentiation, and optimization."),

                // General Physics (FRESH-NS-113)
                Resource("RES_NS113_01", "FRESH-NS-113", "Mechanics and Wave Motion Textbook", "Lecture Notes", "3.4 MB", "Comprehensive exploration of Newtonian mechanics, work-energy theorem, dynamics of systems, and simple harmonic oscillations."),
                Resource("RES_NS113_02", "FRESH-NS-113", "General Physics Midterm Paper - 2025", "Past Exams", "1.1 MB", "Arba Minch university standard physics freshman exam on projectile motion, centripetal forces, and friction coefficients."),

                // General Chemistry (FRESH-NS-114)
                Resource("RES_NS114_01", "FRESH-NS-114", "Chemical Bonding & Stoichiometry Study Guide", "Handouts", "2.7 MB", "Covers covalent and ionic properties, molecular geometry shapes (VSEPR theory), gas laws, and balancing redox balance equations."),

                // General Biology (FRESH-NS-ON1)
                Resource("RES_NSON1_01", "FRESH-NS-ON1", "Cellular Biology and Genetics Lecture Notes", "Lecture Notes", "3.8 MB", "Examines plant and animal organelle structures, cellular respiration cycles, DNA replication steps, and Mendelian inheritance patterns."),

                // Introduction to Statistics (FRESH-NS-ON2)
                Resource("RES_NSON2_01", "FRESH-NS-ON2", "Descriptive Statistics & Probability Distributions", "Lecture Notes", "2.5 MB", "Covers numerical summary calculations (mean, variance), basic probability theories, normal curves, and sample regression lines."),
                Resource("RES_NSON2_02", "FRESH-NS-ON2", "Hypothesis Testing Lab Exercises", "Assignments", "1.2 MB", "Includes practical worksheets for t-tests, chi-square distributions, and confidence interval estimation formulas."),

                // Geography of Ethiopia (FRESH-SS-111)
                Resource("RES_SS111_01", "FRESH-SS-111", "Geography of Ethiopia and the Horn Manual", "Lecture Notes", "2.8 MB", "Comprehensive freshman guide covering topological configurations, climate zones, and demographic trends of East Africa."),

                // General Psychology (FRESH-SS-112)
                Resource("RES_SS112_01", "FRESH-SS-112", "Introduction to Psychology Readings", "Handouts", "1.6 MB", "Selected readings on biological basis of behavior, sensory processes, learning theories, and human memory constructs."),

                // Data Structures & Algorithms (CS-Y2S2-05)
                Resource("RES_CS211_01", "CS-Y2S2-05", "Singly Linked List Manipulation Guide", "Lecture Notes", "2.5 MB", "Node representations, insertion/deletion pointer swapping code templates, and big-O performance analysis."),
                Resource("RES_CS211_02", "CS-Y2S2-05", "Lab Manual 2: Circular Queues with Ring Buffers", "Lab Reports", "1.9 MB", "C++ templates outlining ring-buffer logic, index wrapping (head/tail indices), and exception checks for overflow/underflow."),
                Resource("RES_CS211_03", "CS-Y2S2-05", "Binary Search Tree Past Midterm - 2025", "Past Exams", "1.8 MB", "Midterm paper testing pre-order, post-order, and in-order tree traversals, BST node deletion cases, and maximum heap insertion."),

                // Wireless Communication and Mobile Computing (CS-Y3S2-01) - Previously Mobile Application Development
                Resource("RES_CS322_01", "CS-Y3S2-01", "Jetpack Compose Core Concepts Manual", "Lecture Notes", "3.2 MB", "Explanations of recomposition, remember, rememberSaveable, derivedStateOf, and side effects like LaunchedEffect and DisposableEffect."),
                Resource("RES_CS322_02", "CS-Y3S2-01", "State Management with flows & ViewModel", "Handouts", "2.1 MB", "Detailed manual on flows (StateFlow, SharedFlow), state hoistings with architecture patterns, and lifecycle-aware collectors."),
                Resource("RES_CS322_03", "CS-Y3S2-01", "Clean Architecture in Mobile Handout", "Project Examples", "4.8 MB", "A fully documented sample project showing architectural partitions (Data, Business, UI layers), dependency isolation, and local DBs."),

                // Basic Circuits Analysis (AMIT-EE-211)
                Resource("RES_EE211_01", "AMIT-EE-211", "Ohm's & Kirchhoff's Law Worked Problems", "Handouts", "1.2 MB", "Step-by-step resolution of nodal and mesh analysis equations for diverse resistive DC networks."),
                Resource("RES_EE211_02", "AMIT-EE-211", "Basic Circuit Analysis Past Exam 2024", "Past Exams", "2.3 MB", "Official AMIT electrical department evaluation sheet with questions on transient response networks and Thévenin equivalents."),
                Resource("RES_EE211_03", "AMIT-EE-211", "Fourier Analysis & Continuous Time Signals", "Assignments", "1.1 MB", "Problem set with 15 detailed questions exploring Laplace conversions, discrete Fourier coefficients, and convolutive integrations."),

                // Hydraulics & Water Engineering (AMIT-CE-322)
                Resource("RES_CE322_01", "AMIT-CE-322", "Water Turbine Systems and Flow Dynamics", "Lecture Notes", "5.2 MB", "Advanced fluid mechanics slides outlining turbine efficiency, water-hammer effects, and hydraulic head configurations."),
                Resource("RES_CE322_02", "AMIT-CE-322", "Hydraulics Laboratory Verification Manual", "Lab Reports", "2.1 MB", "Step-by-step instructions on measuring open-channel velocity coefficients, hydraulic jump heights, and pipe friction losses."),

                // Human Anatomy I (CMHS-MD-211)
                Resource("RES_MD211_01", "CMHS-MD-211", "Musculoskeletal Histological Album", "Lecture Notes", "6.1 MB", "High-definition hand drawings and anatomical tags explaining different skeletal, cardiac, and smooth muscle tissues."),
                Resource("RES_MD211_02", "CMHS-MD-211", "Human Bone Landmarks Identification Guide", "Handouts", "3.2 MB", "Visual checklist of cranial bony markers, skeletal configurations, and articular processes for diagnostic anatomy laboratory sessions."),
                Resource("RES_MD211_03", "CMHS-MD-211", "Anatomy I Past Course Final Exam - 2024", "Past Exams", "1.5 MB", "Official AMU clinical medicine final exam testing musculoskeletal structures, cardiovascular supply routes, and neuroanatomy tracts."),

                // Introductory Accounting (COBE-AF-211)
                Resource("RES_AF211_01", "COBE-AF-211", "Double Entry Bookkeeping Guide", "Lecture Notes", "1.4 MB", "Exposing general journal entries, LEDGER accounts balances, and adjusting trials of standard trading businesses."),
                Resource("RES_AF211_02", "COBE-AF-211", "Journal ledger adjustments and balance sheets", "Assignments", "980 KB", "Practical problem workbook for completing general ledger entries, double-entry adjusting accounts, and closing worksheets."),
                Resource("RES_AF211_03", "COBE-AF-211", "Accounting I Departmental Past Final Exam - 2025", "Past Exams", "1.3 MB", "Official regional college evaluation testing merchandise transactions, bad debts provisioning, and audit traits."),

                // Ethiopian Constitutional Law I (SOL-LW-311)
                Resource("RES_LW311_01", "SOL-LW-311", "FDRE Constitution Structural Commentary", "Lecture Notes", "3.5 MB", "A rigorous dissection of the Federal Democratic Republic of Ethiopia’s constitutional design, regional state power layout, and basic human rights foundations."),
                Resource("RES_LW311_02", "SOL-LW-311", "Constitutional Past Exam Paper 2025", "Past Exams", "1.1 MB", "AMU Law center final exam question prompts exploring treaty ratifications and judicial review power balances."),

                // Principles of Agronomy (COAS-PS-211)
                Resource("RES_PS211_01", "COAS-PS-211", "Ethiopian Crop Rotation & Soil Recovery Guidance", "Handouts", "2.4 MB", "Best crop rotation guidelines designed by AMU agricultural station for dryland farming systems, utilizing pulses and cereals."),
                Resource("RES_PS211_02", "COAS-PS-211", "Arba Minch Soil Quality & Crop Yield Worksheet", "Assignments", "1.7 MB", "Practical problem sets investigating crop water requirements, nitrogen replenishment rates in sandy clay soils, and optimal spacing metrics."),

                // Water Technology Institute - Fluid Mechanics I (AWTI-HW-111)
                Resource("RES_HW111_01", "AWTI-HW-111", "Fluid Hydrostatics Lecture Slides", "Lecture Notes", "3.5 MB", "Covers atmospheric pressure scales, manometry indicators, and submerged plane surfaces."),
                Resource("RES_HW111_02", "AWTI-HW-111", "Fluid Mechanics Past Term Midterm - 2024", "Past Exams", "1.4 MB", "Midterm evaluation regarding buoyancy, Archimedes theories, and vortex acceleration calculations."),
                Resource("RES_HW111_03", "AWTI-HW-111", "Nodal flow distribution equations assignment", "Assignments", "850 KB", "Take-home assignment covering boundary layers, laminar stream flows, and Bernoulli applications."),

                // Natural Sciences - General Microbiology (CNCS-BI-111)
                Resource("RES_BI111_01", "CNCS-BI-111", "Bacterial Cell Structures & Staining Handout", "Handouts", "2.3 MB", "Gram-positive vs Gram-negative wall differences and microscope calibration techniques."),
                Resource("RES_BI111_02", "CNCS-BI-111", "Microbiology Lab Report 1: Agar Cultures Prep", "Lab Reports", "1.9 MB", "Detailed laboratory instructions on aseptic preparation on nutrient plates, incubation cycles, and bacterial counting."),

                // Sawla Campus Mechatronics (SC-EM-111)
                Resource("RES_SCEM111_01", "SC-EM-111", "Microcontrollers and Sensor Interfacing Guides", "Lecture Notes", "4.1 MB", "PWM output gates, ADC configurations, register configurations, and mechanical relay switches."),
                Resource("RES_SCEM111_02", "SC-EM-111", "Mechatronics System Integration Project Example", "Project Examples", "6.4 MB", "Documented project layout of an automated dryland solar tracker system, utilizing photo-resistors and servo controllers.")
            )
            resourceDao.insertResources(resources)
        }
    }

    suspend fun addCustomResource(resource: Resource) {
        resourceDao.insertResources(listOf(resource))
    }

    suspend fun addCustomCourse(course: Course) {
        courseDao.insertCourses(listOf(course))
    }
}
