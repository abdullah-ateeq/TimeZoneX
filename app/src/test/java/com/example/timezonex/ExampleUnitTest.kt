package com.example.timezonex

import com.example.timezonex.data.CityDatabase
import com.example.timezonex.data.CityRepository
import com.example.timezonex.data.FavoritesRepository
import com.example.timezonex.data.InMemoryPreferenceStore
import com.example.timezonex.data.SettingsRepository
import com.example.timezonex.domain.calculator.BusinessHourCalculator
import com.example.timezonex.domain.calculator.DayNightCalculator
import com.example.timezonex.domain.calculator.DstCalculator
import com.example.timezonex.domain.calculator.TimeDifferenceCalculator
import com.example.timezonex.domain.calculator.TimeFormatter
import com.example.timezonex.domain.calculator.TimeZoneCalculator
import com.example.timezonex.domain.model.CityTimeZone
import com.example.timezonex.domain.model.ThemePreference
import com.example.timezonex.domain.model.TimeFormatPreference
import com.example.timezonex.domain.model.UserSettings
import com.example.timezonex.ui.viewmodel.TimeZoneViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Comprehensive unit test suite covering all 46 required functional & critical scenarios
 * for TimeZoneX.
 *
 * Uses deterministic fixed [Instant] values so tests never depend on wall-clock execution time.
 */
class ExampleUnitTest {

    // Fixed reference instant: Saturday, 24 October 2026 at 04:30:00 UTC
    // At this instant:
    // - Karachi (Asia/Karachi, UTC+05:00): 09:30 AM, Saturday 24 Oct 2026 (Standard Time)
    // - London (Europe/London, BST UTC+01:00 until Oct 25 2026): 05:30 AM, Saturday 24 Oct 2026 (DST Active)
    // - New York (America/New_York, EDT UTC-04:00 until Nov 1 2026): 12:30 AM, Saturday 24 Oct 2026 (DST Active)
    // - Tokyo (Asia/Tokyo, JST UTC+09:00): 01:30 PM (13:30), Saturday 24 Oct 2026 (Standard Time)
    private val fixedOctInstant: Instant = Instant.parse("2026-10-24T04:30:00Z")

    // Fixed winter instant (Standard Time in Northern Hemisphere): Thursday, 15 January 2026 at 04:30:00 UTC
    // - Karachi: 09:30 AM (UTC+05:00)
    // - London: 04:30 AM (UTC+00:00, Standard Time)
    // - New York: 11:30 PM Jan 14 (UTC-05:00, Standard Time)
    // - Tokyo: 01:30 PM (UTC+09:00, Standard Time)
    private val fixedJanInstant: Instant = Instant.parse("2026-01-15T04:30:00Z")

    private lateinit var calculator: TimeZoneCalculator
    private lateinit var diffCalculator: TimeDifferenceCalculator
    private lateinit var dstCalculator: DstCalculator
    private lateinit var dayNightCalculator: DayNightCalculator
    private lateinit var businessHourCalculator: BusinessHourCalculator
    private lateinit var timeFormatter: TimeFormatter
    private lateinit var cityDatabase: CityDatabase
    private lateinit var preferenceStore: InMemoryPreferenceStore
    private lateinit var cityRepository: CityRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var favoritesRepository: FavoritesRepository

    private fun loadAssetsCitiesJson(): String {
        val candidates = listOf(
            File("src/main/assets/cities.json"),
            File("app/src/main/assets/cities.json"),
            File("/Users/abdullah/StudioProjects/TimeZoneX/app/src/main/assets/cities.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Could not locate assets/cities.json for unit tests")
        return file.readText()
    }

    @Before
    fun setUp() {
        timeFormatter = TimeFormatter()
        diffCalculator = TimeDifferenceCalculator()
        dstCalculator = DstCalculator()
        dayNightCalculator = DayNightCalculator()
        businessHourCalculator = BusinessHourCalculator(timeFormatter)
        calculator = TimeZoneCalculator(
            timeDifferenceCalculator = diffCalculator,
            dstCalculator = dstCalculator,
            dayNightCalculator = dayNightCalculator,
            businessHourCalculator = businessHourCalculator,
            timeFormatter = timeFormatter
        )
        val jsonText = loadAssetsCitiesJson()
        cityDatabase = CityDatabase(jsonProvider = { jsonText })
        preferenceStore = InMemoryPreferenceStore()
        cityRepository = CityRepository(cityDatabase, preferenceStore)
        settingsRepository = SettingsRepository(preferenceStore)
        favoritesRepository = FavoritesRepository(preferenceStore)
    }

    private fun createViewModel(
        localZoneId: ZoneId = ZoneId.of("Asia/Karachi"),
        initialInstant: Instant = fixedOctInstant,
        system24Hour: Boolean = false
    ): TimeZoneViewModel {
        return TimeZoneViewModel(
            cityRepository = cityRepository,
            settingsRepository = settingsRepository,
            favoritesRepository = favoritesRepository,
            calculator = calculator,
            systemIs24HourProvider = { system24Hour },
            localZoneIdProvider = { localZoneId },
            initialInstant = initialInstant
        )
    }

    // 1. System time-zone detection
    @Test
    fun test01_systemTimeZoneDetection() {
        val systemZone = calculator.getSystemZoneId()
        assertNotNull(systemZone)
        assertTrue(systemZone.id.isNotBlank())

        val vm = createViewModel(localZoneId = ZoneId.of("Asia/Karachi"))
        assertEquals("Asia/Karachi", vm.uiState.value.localZoneId)
        assertEquals("Karachi", vm.uiState.value.localCityName)
    }

    // 2. City database loading (100+ offline cities)
    @Test
    fun test02_cityDatabaseLoading_containsAtLeast100Cities() {
        val cities = cityDatabase.loadCities()
        assertTrue("Expected at least 100 cities, found ${cities.size}", cities.size >= 100)
        assertTrue(cities.any { it.id == "karachi" })
        assertTrue(cities.any { it.id == "london" })
        assertTrue(cities.any { it.id == "new_york" })
        assertTrue(cities.any { it.id == "tokyo" })
        assertTrue(cities.any { it.id == "kathmandu" })
        assertTrue(cities.any { it.id == "adelaide" })
    }

    // 3. City search (by city name, country name, and zone ID)
    @Test
    fun test03_citySearch_byNameCountryAndZoneId() {
        val byCity = cityRepository.searchCities("new york")
        assertTrue(byCity.any { it.id == "new_york" })

        val byZoneId = cityRepository.searchCities("america/new_york")
        assertTrue(byZoneId.any { it.id == "new_york" })

        val byCountry = cityRepository.searchCities("United Kingdom")
        assertTrue(byCountry.any { it.id == "london" })
    }

    // 4. City aliases
    @Test
    fun test04_citySearch_byAliases() {
        val ukSearch = cityRepository.searchCities("uk")
        assertTrue(ukSearch.any { it.id == "london" })

        val nycSearch = cityRepository.searchCities("nyc")
        assertTrue(nycSearch.any { it.id == "new_york" })

        val bombaySearch = cityRepository.searchCities("bombay")
        assertTrue(bombaySearch.any { it.id == "mumbai" })
    }

    // 5. Add city
    @Test
    fun test05_addCity() {
        val vm = createViewModel()
        val added = vm.addCity("dubai")
        assertTrue(added)
        assertTrue(vm.uiState.value.selectedCities.any { it.city.id == "dubai" })
    }

    // 6. Remove city
    @Test
    fun test06_removeCity() {
        val vm = createViewModel()
        assertTrue(vm.uiState.value.selectedCities.any { it.city.id == "tokyo" })
        val countBefore = vm.uiState.value.selectedCities.size

        vm.removeCity("tokyo")

        val stateAfter = vm.uiState.value
        assertFalse(stateAfter.selectedCities.any { it.city.id == "tokyo" })
        assertEquals(countBefore - 1, stateAfter.selectedCities.size)
        // Other cities remain untouched
        assertTrue(stateAfter.selectedCities.any { it.city.id == "london" })
        assertTrue(stateAfter.selectedCities.any { it.city.id == "new_york" })
    }

    // 7. Duplicate prevention
    @Test
    fun test07_duplicatePrevention() {
        val vm = createViewModel()
        // London is already in defaults
        val londonCountBefore = vm.uiState.value.selectedCities.count { it.city.id == "london" }
        assertEquals(1, londonCountBefore)

        val addedAgain = vm.addCity("london")
        assertFalse(addedAgain)

        val londonCountAfter = vm.uiState.value.selectedCities.count { it.city.id == "london" }
        assertEquals(1, londonCountAfter)
    }

    // 8. Reordering
    @Test
    fun test08_reorderCities() {
        val vm = createViewModel(localZoneId = ZoneId.of("UTC"))
        // Set explicit 3 cities: London, Tokyo, New York
        vm.uiState.value.selectedCities.map { it.city.id }.forEach { vm.removeCity(it) }
        vm.addCity("london")
        vm.addCity("tokyo")
        vm.addCity("new_york")

        assertEquals(
            listOf("london", "tokyo", "new_york"),
            vm.uiState.value.selectedCities.map { it.city.id }
        )

        // Move New York (index 2) to top (index 0)
        vm.reorderCity(2, 0)

        assertEquals(
            listOf("new_york", "london", "tokyo"),
            vm.uiState.value.selectedCities.map { it.city.id }
        )
    }

    // 9. Favorites
    @Test
    fun test09_favoritesToggleAndClear() {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.favoriteCityIds.contains("singapore"))

        vm.toggleFavorite("singapore")
        assertTrue(vm.uiState.value.favoriteCityIds.contains("singapore"))

        vm.toggleFavorite("singapore")
        assertFalse(vm.uiState.value.favoriteCityIds.contains("singapore"))

        vm.toggleFavorite("dubai")
        vm.toggleFavorite("paris")
        assertEquals(2, vm.uiState.value.favoriteCityIds.size)

        vm.clearFavorites()
        assertTrue(vm.uiState.value.favoriteCityIds.isEmpty())
    }

    // 10. Maximum city limit (12 cities)
    @Test
    fun test10_maximumCityLimit_blocks13thCityWithMessage() {
        val vm = createViewModel()
        // Clear existing
        vm.uiState.value.selectedCities.map { it.city.id }.forEach { vm.removeCity(it) }

        val twelveIds = listOf(
            "karachi", "london", "new_york", "tokyo",
            "dubai", "paris", "berlin", "sydney",
            "singapore", "toronto", "cairo", "sao_paulo"
        )
        twelveIds.forEach { id ->
            assertTrue("Should allow adding $id", vm.addCity(id))
        }
        assertEquals(12, vm.uiState.value.selectedCities.size)

        // Attempt to add 13th city
        val added13th = vm.addCity("mumbai")
        assertFalse(added13th)
        assertEquals(12, vm.uiState.value.selectedCities.size)
        assertEquals("You can compare up to 12 cities.", vm.uiState.value.errorMessage)
    }

    // 11. Current time conversion (Same Instant -> Multiple Zones)
    @Test
    fun test11_currentTimeConversion_sameInstantAcrossMultipleZones() {
        val instant = fixedOctInstant // 2026-10-24T04:30:00Z
        val karachi = calculator.convertInstantToZone(instant, "Asia/Karachi")
        val london = calculator.convertInstantToZone(instant, "Europe/London")
        val newYork = calculator.convertInstantToZone(instant, "America/New_York")
        val tokyo = calculator.convertInstantToZone(instant, "Asia/Tokyo")

        // Verify all ZonedDateTimes represent the exact same Instant
        assertEquals(instant, karachi.toInstant())
        assertEquals(instant, london.toInstant())
        assertEquals(instant, newYork.toInstant())
        assertEquals(instant, tokyo.toInstant())

        // Verify exact local times
        assertEquals(LocalTime.of(9, 30), karachi.toLocalTime())
        assertEquals(LocalTime.of(5, 30), london.toLocalTime())
        assertEquals(LocalTime.of(0, 30), newYork.toLocalTime())
        assertEquals(LocalTime.of(13, 30), tokyo.toLocalTime())
    }

    // 12. UTC offset calculation
    @Test
    fun test12_utcOffsetCalculation() {
        val offsetSeconds = calculator.calculateUtcOffsetSeconds(
            fixedOctInstant,
            ZoneId.of("Asia/Karachi")
        )
        assertEquals(5 * 3600, offsetSeconds)
        assertEquals("UTC +05:00", timeFormatter.formatUtcOffset(offsetSeconds))
    }

    // 13. Positive offset
    @Test
    fun test13_positiveOffset() {
        val tokyoOffset = calculator.calculateUtcOffsetSeconds(
            fixedOctInstant,
            ZoneId.of("Asia/Tokyo")
        )
        assertEquals(9 * 3600, tokyoOffset)
        assertEquals("UTC +09:00", timeFormatter.formatUtcOffset(tokyoOffset))
    }

    // 14. Negative offset
    @Test
    fun test14_negativeOffset() {
        val nyOffset = calculator.calculateUtcOffsetSeconds(
            fixedOctInstant,
            ZoneId.of("America/New_York")
        )
        assertEquals(-4 * 3600, nyOffset)
        assertEquals("UTC -04:00", timeFormatter.formatUtcOffset(nyOffset))
    }

    // 15. Zero offset
    @Test
    fun test15_zeroOffset() {
        val londonWinterOffset = calculator.calculateUtcOffsetSeconds(
            fixedJanInstant,
            ZoneId.of("Europe/London")
        )
        assertEquals(0, londonWinterOffset)
        assertEquals("UTC +00:00", timeFormatter.formatUtcOffset(londonWinterOffset))
    }

    // 16. Half-hour offset (India & Adelaide)
    @Test
    fun test16_halfHourOffset_indiaAndAdelaide() {
        val kolkataOffset = calculator.calculateUtcOffsetSeconds(
            fixedOctInstant,
            ZoneId.of("Asia/Kolkata")
        )
        assertEquals((5 * 3600) + (30 * 60), kolkataOffset)
        assertEquals("UTC +05:30", timeFormatter.formatUtcOffset(kolkataOffset))

        // Adelaide in June (Winter Standard Time ACST = UTC+09:30)
        val juneInstant = Instant.parse("2026-06-15T04:00:00Z")
        val adelaideWinterOffset = calculator.calculateUtcOffsetSeconds(
            juneInstant,
            ZoneId.of("Australia/Adelaide")
        )
        assertEquals((9 * 3600) + (30 * 60), adelaideWinterOffset)
        assertEquals("UTC +09:30", timeFormatter.formatUtcOffset(adelaideWinterOffset))
    }

    // 17. 45-minute offset (Kathmandu)
    @Test
    fun test17_fortyFiveMinuteOffset_kathmandu() {
        val kathmanduOffset = calculator.calculateUtcOffsetSeconds(
            fixedOctInstant,
            ZoneId.of("Asia/Kathmandu")
        )
        assertEquals((5 * 3600) + (45 * 60), kathmanduOffset)
        assertEquals("UTC +05:45", timeFormatter.formatUtcOffset(kathmanduOffset))
    }

    // 18. Time difference (whole hours, half hours, 45 minutes)
    @Test
    fun test18_timeDifferenceCalculationAndFormatting() {
        val karachiZone = ZoneId.of("Asia/Karachi") // UTC+05:00

        // Tokyo (UTC+09:00) vs Karachi (UTC+05:00) -> +240 min -> "4 hours ahead"
        val tokyoDiff = calculator.calculateTimeDifferenceMinutes(
            fixedOctInstant,
            karachiZone,
            ZoneId.of("Asia/Tokyo")
        )
        assertEquals(240, tokyoDiff)
        assertEquals("4 hours ahead", diffCalculator.formatDifference(tokyoDiff))

        // New York in Oct (UTC-04:00) vs Karachi (UTC+05:00) -> -540 min -> "9 hours behind"
        val nyDiff = calculator.calculateTimeDifferenceMinutes(
            fixedOctInstant,
            karachiZone,
            ZoneId.of("America/New_York")
        )
        assertEquals(-540, nyDiff)
        assertEquals("9 hours behind", diffCalculator.formatDifference(nyDiff))

        // Kathmandu (UTC+05:45) vs Karachi (UTC+05:00) -> +45 min -> "45 minutes ahead"
        val ktmDiff = calculator.calculateTimeDifferenceMinutes(
            fixedOctInstant,
            karachiZone,
            ZoneId.of("Asia/Kathmandu")
        )
        assertEquals(45, ktmDiff)
        assertEquals("45 minutes ahead", diffCalculator.formatDifference(ktmDiff))

        // Adelaide in June (UTC+09:30) vs Karachi (UTC+05:00) -> +270 min -> "4 hours 30 minutes ahead"
        val juneInstant = Instant.parse("2026-06-15T04:00:00Z")
        val adelaideDiff = calculator.calculateTimeDifferenceMinutes(
            juneInstant,
            karachiZone,
            ZoneId.of("Australia/Adelaide")
        )
        assertEquals(270, adelaideDiff)
        assertEquals("4 hours 30 minutes ahead", diffCalculator.formatDifference(adelaideDiff))
    }

    // 19. Same time ("Same time", never "0 hours ahead")
    @Test
    fun test19_sameTimeZoneDisplay() {
        val karachiZone = ZoneId.of("Asia/Karachi")
        val tashkentZone = ZoneId.of("Asia/Tashkent") // Also UTC+05:00
        val diff = calculator.calculateTimeDifferenceMinutes(
            fixedOctInstant,
            karachiZone,
            tashkentZone
        )
        assertEquals(0, diff)
        assertEquals("Same time", diffCalculator.formatDifference(diff))
    }

    // 20. Date difference (+1 day / -1 day)
    @Test
    fun test20_dateDifferenceCalculation() {
        // 2026-10-24T16:30:00Z:
        // Karachi (UTC+5): 21:30 (9:30 PM) Saturday, 24 Oct
        // Tokyo (UTC+9): 01:30 AM Sunday, 25 Oct (+1 day)
        val eveningInstant = Instant.parse("2026-10-24T16:30:00Z")
        val dayDiff = calculator.calculateDayDifference(
            eveningInstant,
            ZoneId.of("Asia/Karachi"),
            ZoneId.of("Asia/Tokyo")
        )
        assertEquals(1, dayDiff)
        assertEquals("+1 day", diffCalculator.formatDayDifferenceBadge(dayDiff))
    }

    // 21. Midnight crossing (11:30 PM Saturday vs 12:30 AM Sunday)
    @Test
    fun test21_midnightCrossing() {
        // 2026-10-24T18:30:00Z:
        // Karachi (UTC+5): 11:30 PM Saturday, 24 Oct 2026
        // Dhaka (UTC+6): 12:30 AM Sunday, 25 Oct 2026
        val instant = Instant.parse("2026-10-24T18:30:00Z")
        val karachiZoned = calculator.convertInstantToZone(instant, "Asia/Karachi")
        val dhakaCity = cityRepository.getCityById("dhaka")!!

        val dhakaState = calculator.calculateCityTimeState(
            instant = instant,
            referenceZonedDateTime = karachiZoned,
            city = dhakaCity,
            use24HourFormat = false
        )!!

        assertEquals(LocalDate.of(2026, 10, 24), karachiZoned.toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 25), dhakaState.localDateTime.toLocalDate())
        assertEquals("12:30 AM", dhakaState.formattedTime)
        assertEquals(1, dhakaState.dayDifference)
        assertEquals("+1 day", dhakaState.formattedDayDifference)
    }

    // 22. Day/night indicator (06:00–17:59 Day, 18:00–05:59 Night)
    @Test
    fun test22_dayNightIndicatorBoundaries() {
        assertFalse(dayNightCalculator.isDayTime(LocalTime.of(5, 59)))
        assertTrue(dayNightCalculator.isDayTime(LocalTime.of(6, 0)))
        assertTrue(dayNightCalculator.isDayTime(LocalTime.of(12, 30)))
        assertTrue(dayNightCalculator.isDayTime(LocalTime.of(17, 59)))
        assertFalse(dayNightCalculator.isDayTime(LocalTime.of(18, 0)))
        assertFalse(dayNightCalculator.isDayTime(LocalTime.of(23, 45)))
        assertEquals("☀ Day", dayNightCalculator.getDayNightLabel(true))
        assertEquals("☾ Night", dayNightCalculator.getDayNightLabel(false))
    }

    // 23. DST active
    @Test
    fun test23_dstActiveDetection() {
        val summerInstant = Instant.parse("2026-07-15T12:00:00Z")
        assertTrue(dstCalculator.isDstActive(summerInstant, ZoneId.of("America/New_York")))
        assertTrue(dstCalculator.isDstActive(summerInstant, ZoneId.of("Europe/London")))
        assertEquals("DST Active", dstCalculator.getDstStatusLabel(true))
    }

    // 24. Standard time
    @Test
    fun test24_standardTimeDetection() {
        assertFalse(dstCalculator.isDstActive(fixedJanInstant, ZoneId.of("America/New_York")))
        assertFalse(dstCalculator.isDstActive(fixedJanInstant, ZoneId.of("Europe/London")))
        assertFalse(dstCalculator.isDstActive(fixedOctInstant, ZoneId.of("Asia/Karachi")))
        assertFalse(dstCalculator.isDstActive(fixedOctInstant, ZoneId.of("Asia/Tokyo")))
        assertEquals("Standard Time", dstCalculator.getDstStatusLabel(false))
    }

    // 25. DST transition (Spring & Autumn transitions for America/New_York and Europe/London)
    @Test
    fun test25_dstTransitions_springAndAutumn() {
        val nyZone = ZoneId.of("America/New_York")
        // 2026 US Spring forward: Sunday, March 8, 2026 at 07:00 UTC (2:00 AM EST -> 3:00 AM EDT)
        val beforeSpringNy = Instant.parse("2026-03-08T06:59:00Z")
        val afterSpringNy = Instant.parse("2026-03-08T07:01:00Z")

        assertFalse(dstCalculator.isDstActive(beforeSpringNy, nyZone))
        assertEquals(-5 * 3600, calculator.calculateUtcOffsetSeconds(beforeSpringNy, nyZone))

        assertTrue(dstCalculator.isDstActive(afterSpringNy, nyZone))
        assertEquals(-4 * 3600, calculator.calculateUtcOffsetSeconds(afterSpringNy, nyZone))

        // 2026 US Fall back: Sunday, November 1, 2026 at 06:00 UTC (2:00 AM EDT -> 1:00 AM EST)
        val beforeFallNy = Instant.parse("2026-11-01T05:59:00Z")
        val afterFallNy = Instant.parse("2026-11-01T06:01:00Z")

        assertTrue(dstCalculator.isDstActive(beforeFallNy, nyZone))
        assertEquals(-4 * 3600, calculator.calculateUtcOffsetSeconds(beforeFallNy, nyZone))

        assertFalse(dstCalculator.isDstActive(afterFallNy, nyZone))
        assertEquals(-5 * 3600, calculator.calculateUtcOffsetSeconds(afterFallNy, nyZone))
    }

    // 26. London city calculation
    @Test
    fun test26_londonCalculation() {
        val london = cityRepository.getCityById("london")!!
        val refKarachi = calculator.convertInstantToZone(fixedJanInstant, "Asia/Karachi")
        val state = calculator.calculateCityTimeState(fixedJanInstant, refKarachi, london)!!

        assertEquals("London", state.city.cityName)
        assertEquals("United Kingdom", state.city.countryName)
        assertEquals("Europe/London", state.city.zoneId)
        assertEquals("04:30 AM", state.formattedTime)
        assertEquals("UTC +00:00", state.formattedUtcOffset)
        assertEquals("5 hours behind", state.formattedDifference)
        assertFalse(state.isDst)
        assertFalse(state.isDay)
    }

    // 27. New York city calculation
    @Test
    fun test27_newYorkCalculation() {
        val newYork = cityRepository.getCityById("new_york")!!
        val refKarachi = calculator.convertInstantToZone(fixedOctInstant, "Asia/Karachi")
        val state = calculator.calculateCityTimeState(fixedOctInstant, refKarachi, newYork)!!

        assertEquals("New York", state.city.cityName)
        assertEquals("America/New_York", state.city.zoneId)
        assertEquals("12:30 AM", state.formattedTime)
        assertEquals("UTC -04:00", state.formattedUtcOffset)
        assertEquals("9 hours behind", state.formattedDifference)
        assertTrue(state.isDst)
        assertFalse(state.isDay)
    }

    // 28. Tokyo city calculation
    @Test
    fun test28_tokyoCalculation() {
        val tokyo = cityRepository.getCityById("tokyo")!!
        val refKarachi = calculator.convertInstantToZone(fixedOctInstant, "Asia/Karachi")
        val state = calculator.calculateCityTimeState(fixedOctInstant, refKarachi, tokyo)!!

        assertEquals("Tokyo", state.city.cityName)
        assertEquals("Asia/Tokyo", state.city.zoneId)
        assertEquals("01:30 PM", state.formattedTime)
        assertEquals("UTC +09:00", state.formattedUtcOffset)
        assertEquals("4 hours ahead", state.formattedDifference)
        assertFalse(state.isDst)
        assertTrue(state.isDay)
    }

    // 29. Karachi city calculation
    @Test
    fun test29_karachiCalculation() {
        val karachi = cityRepository.getCityById("karachi")!!
        val refKarachi = calculator.convertInstantToZone(fixedOctInstant, "Asia/Karachi")
        val state = calculator.calculateCityTimeState(fixedOctInstant, refKarachi, karachi)!!

        assertEquals("Karachi", state.city.cityName)
        assertEquals("Asia/Karachi", state.city.zoneId)
        assertEquals("09:30 AM", state.formattedTime)
        assertEquals("UTC +05:00", state.formattedUtcOffset)
        assertEquals("Same time", state.formattedDifference)
        assertFalse(state.isDst)
        assertTrue(state.isDay)
    }

    // 30. Kathmandu city calculation (UTC +05:45)
    @Test
    fun test30_kathmanduCalculation() {
        val kathmandu = cityRepository.getCityById("kathmandu")!!
        val refKarachi = calculator.convertInstantToZone(fixedOctInstant, "Asia/Karachi")
        val state = calculator.calculateCityTimeState(fixedOctInstant, refKarachi, kathmandu)!!

        assertEquals("Kathmandu", state.city.cityName)
        assertEquals("10:15 AM", state.formattedTime)
        assertEquals("UTC +05:45", state.formattedUtcOffset)
        assertEquals("45 minutes ahead", state.formattedDifference)
    }

    // 31. Adelaide city calculation (including DST changes between ACST +09:30 and ACDT +10:30)
    @Test
    fun test31_adelaideCalculation_winterAndSummerDst() {
        val adelaide = cityRepository.getCityById("adelaide")!!
        val juneInstant = Instant.parse("2026-06-15T04:00:00Z") // Winter in Southern Hemisphere -> UTC+09:30
        val refKarachiJune = calculator.convertInstantToZone(juneInstant, "Asia/Karachi")
        val winterState = calculator.calculateCityTimeState(juneInstant, refKarachiJune, adelaide)!!

        assertEquals("UTC +09:30", winterState.formattedUtcOffset)
        assertFalse(winterState.isDst)
        assertEquals("4 hours 30 minutes ahead", winterState.formattedDifference)

        // October in Southern Hemisphere -> ACDT (DST active, UTC+10:30)
        val refKarachiOct = calculator.convertInstantToZone(fixedOctInstant, "Asia/Karachi")
        val summerState = calculator.calculateCityTimeState(fixedOctInstant, refKarachiOct, adelaide)!!
        assertEquals("UTC +10:30", summerState.formattedUtcOffset)
        assertTrue(summerState.isDst)
        assertEquals("5 hours 30 minutes ahead", summerState.formattedDifference)
    }

    // 32. UTC standard zone
    @Test
    fun test32_utcZoneCalculation() {
        val utcCity = cityRepository.getCityById("utc")!!
        val refKarachi = calculator.convertInstantToZone(fixedOctInstant, "Asia/Karachi")
        val state = calculator.calculateCityTimeState(fixedOctInstant, refKarachi, utcCity)!!

        assertEquals("04:30 AM", state.formattedTime)
        assertEquals("UTC +00:00", state.formattedUtcOffset)
        assertEquals("5 hours behind", state.formattedDifference)
        assertFalse(state.isDst)
    }

    // 33. 12-hour format
    @Test
    fun test33_twelveHourFormatting() {
        val morning = LocalTime.of(9, 30)
        val afternoon = LocalTime.of(14, 30)
        assertEquals("09:30 AM", timeFormatter.formatTime(morning, use24Hour = false))
        assertEquals("02:30 PM", timeFormatter.formatTime(afternoon, use24Hour = false))
    }

    // 34. 24-hour format
    @Test
    fun test34_twentyFourHourFormatting() {
        val morning = LocalTime.of(9, 30)
        val afternoon = LocalTime.of(14, 30)
        assertEquals("09:30", timeFormatter.formatTime(morning, use24Hour = true))
        assertEquals("14:30", timeFormatter.formatTime(afternoon, use24Hour = true))
    }

    // 35. Seconds visibility
    @Test
    fun test35_secondsVisibility() {
        val time = LocalTime.of(9, 30, 45)
        assertEquals("09:30:45 AM", timeFormatter.formatTime(time, use24Hour = false, showSeconds = true))
        assertEquals("09:30:45", timeFormatter.formatTime(time, use24Hour = true, showSeconds = true))
    }

    // 36. Business hours status check
    @Test
    fun test36_businessHoursCheck() {
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(17, 0)
        assertFalse(businessHourCalculator.isWithinBusinessHours(LocalTime.of(8, 59), start, end))
        assertTrue(businessHourCalculator.isWithinBusinessHours(LocalTime.of(9, 0), start, end))
        assertTrue(businessHourCalculator.isWithinBusinessHours(LocalTime.of(14, 15), start, end))
        assertFalse(businessHourCalculator.isWithinBusinessHours(LocalTime.of(17, 0), start, end))
    }

    // 37. Business-hour overlap calculation
    @Test
    fun test37_businessHourOverlapBetweenKarachiAndLondon() {
        // In January: Karachi is UTC+5, London is UTC+0 (4 hours difference in summer, 5 hours in winter)
        // Wait: in January, when London is 09:00–12:00, Karachi is 14:00–17:00 (2:00 PM – 5:00 PM)
        val karachi = cityRepository.getCityById("karachi")!!
        val london = cityRepository.getCityById("london")!!

        val overlap = businessHourCalculator.findOverlap(
            referenceInstant = fixedJanInstant,
            referenceZoneId = ZoneId.of("Asia/Karachi"),
            referenceCity = karachi,
            selectedCities = listOf(london),
            workingStart = LocalTime.of(9, 0),
            workingEnd = LocalTime.of(17, 0),
            use24HourFormat = false
        )

        assertTrue(overlap.hasOverlap)
        assertEquals(180, overlap.overlapDurationMinutes) // 3 hours overlap
        assertEquals("2:00 PM – 5:00 PM", overlap.referenceFormattedRange)
    }

    // 38. Cross-midnight overlap
    @Test
    fun test38_crossMidnightWorkingHoursAndOverlap() {
        // Night shift window: 22:00 to 06:00
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(6, 0)
        assertTrue(businessHourCalculator.isWithinBusinessHours(LocalTime.of(23, 30), start, end))
        assertTrue(businessHourCalculator.isWithinBusinessHours(LocalTime.of(2, 15), start, end))
        assertFalse(businessHourCalculator.isWithinBusinessHours(LocalTime.of(12, 0), start, end))

        val karachi = cityRepository.getCityById("karachi")!!
        val dubai = cityRepository.getCityById("dubai")!!
        val overlap = businessHourCalculator.findOverlap(
            referenceInstant = fixedJanInstant,
            referenceZoneId = ZoneId.of("Asia/Karachi"),
            referenceCity = karachi,
            selectedCities = listOf(dubai),
            workingStart = start,
            workingEnd = end,
            use24HourFormat = true
        )
        assertTrue(overlap.hasOverlap)
        assertTrue(overlap.overlapDurationMinutes > 0)
    }

    // 39. Settings persistence
    @Test
    fun test39_settingsPersistence() {
        val custom = UserSettings(
            theme = ThemePreference.DARK,
            dynamicColor = false,
            timeFormat = TimeFormatPreference.HOURS_24,
            showSeconds = true,
            showTimeZoneId = true,
            showUtcOffset = false,
            showDstStatus = false,
            maxCities = 8,
            showDifference = false,
            showDayNight = false,
            showPositionNumbers = true,
            showBusinessHours = true,
            businessStartHour = 8,
            businessStartMinute = 30,
            businessEndHour = 18,
            businessEndMinute = 0,
            restoreLastSelection = true
        )
        settingsRepository.saveSettings(custom)
        val loaded = settingsRepository.loadSettings()
        assertEquals(custom, loaded)
    }

    // 40. City persistence across app restart
    @Test
    fun test40_cityPersistenceAcrossRestart() {
        val vm1 = createViewModel()
        vm1.uiState.value.selectedCities.map { it.city.id }.forEach { vm1.removeCity(it) }
        vm1.addCity("london")
        vm1.addCity("tokyo")
        vm1.addCity("new_york")
        vm1.addCity("dubai")

        // Simulate app restart by creating a new ViewModel backed by the same PreferenceStore
        val vm2 = createViewModel()
        assertEquals(
            listOf("london", "tokyo", "new_york", "dubai"),
            vm2.uiState.value.selectedCities.map { it.city.id }
        )
    }

    // 41. Favorite persistence
    @Test
    fun test41_favoritePersistenceAcrossRestart() {
        val vm1 = createViewModel()
        vm1.toggleFavorite("new_york")
        vm1.toggleFavorite("dubai")
        vm1.toggleFavorite("singapore")

        val vm2 = createViewModel()
        assertEquals(
            setOf("new_york", "dubai", "singapore"),
            vm2.uiState.value.favoriteCityIds
        )
    }

    // 42. City order persistence across restart
    @Test
    fun test42_cityOrderPersistenceAcrossRestart() {
        val vm1 = createViewModel()
        vm1.uiState.value.selectedCities.map { it.city.id }.forEach { vm1.removeCity(it) }
        vm1.addCity("london")
        vm1.addCity("tokyo")
        vm1.addCity("new_york")

        // Move New York (index 2) to top (index 0)
        vm1.reorderCity(2, 0)
        assertEquals(
            listOf("new_york", "london", "tokyo"),
            vm1.uiState.value.selectedCities.map { it.city.id }
        )

        // Restart ViewModel
        val vm2 = createViewModel()
        assertEquals(
            listOf("new_york", "london", "tokyo"),
            vm2.uiState.value.selectedCities.map { it.city.id }
        )
    }

    // 43. Reset settings
    @Test
    fun test43_resetSettings() {
        val vm = createViewModel()
        vm.setThemePreference(ThemePreference.DARK)
        vm.setShowSeconds(true)
        vm.setShowTimeZoneId(true)
        assertEquals(ThemePreference.DARK, vm.uiState.value.settings.theme)
        assertTrue(vm.uiState.value.settings.showSeconds)

        vm.resetSettings()
        assertEquals(UserSettings(), vm.uiState.value.settings)
    }

    // 44. Empty search returns all cities
    @Test
    fun test44_emptySearchReturnsAllCities() {
        val all = cityRepository.getAllCities()
        val emptySearch = cityRepository.searchCities("   ")
        assertEquals(all.size, emptySearch.size)
    }

    // 45. No search results for non-existent query ("xyz123")
    @Test
    fun test45_noSearchResultsForInvalidQuery() {
        val vm = createViewModel()
        vm.updateSearchQuery("xyz123")
        assertTrue(vm.uiState.value.searchResults.isEmpty())
    }

    // 46. Offline operation & invalid time-zone resilience
    @Test
    fun test46_offlineOperationAndInvalidZoneResilience() {
        val malformedAndMixedJson = """
            [
              {"id": "valid_london", "cityName": "London", "countryName": "UK", "countryCode": "GB", "zoneId": "Europe/London"},
              {"id": "invalid_zone", "cityName": "Atlantis", "countryName": "Ocean", "countryCode": "XX", "zoneId": "Invalid/Atlantis_Zone"},
              {"id": "valid_tokyo", "cityName": "Tokyo", "countryName": "Japan", "countryCode": "JP", "zoneId": "Asia/Tokyo"}
            ]
        """
        val db = CityDatabase(jsonProvider = { malformedAndMixedJson })
        val loaded = db.loadCities()

        // Invalid zone is skipped gracefully without crashing
        assertEquals(2, loaded.size)
        assertEquals(listOf("valid_london", "valid_tokyo"), loaded.map { it.id })
        assertNull(calculator.resolveZoneId("Invalid/Atlantis_Zone"))
    }
}
