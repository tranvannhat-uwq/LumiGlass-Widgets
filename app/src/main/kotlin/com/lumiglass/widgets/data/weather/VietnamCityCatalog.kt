package com.lumiglass.widgets.data.weather

data class VietnamCity(
    val id: String,
    val nameVi: String,
    val regionVi: String,
    val latitude: Double,
    val longitude: Double
)

object VietnamCityCatalog {

    val CITIES: List<VietnamCity> = listOf(
        // Miền Bắc
        VietnamCity("hanoi", "Hà Nội", "Miền Bắc", 21.0285, 105.8542),
        VietnamCity("haiphong", "Hải Phòng", "Miền Bắc", 20.8449, 106.6881),
        VietnamCity("halong", "Hạ Long (Quảng Ninh)", "Miền Bắc", 20.9599, 107.0425),
        VietnamCity("sapa", "Sa Pa (Lào Cai)", "Miền Bắc", 22.3364, 103.8438),
        VietnamCity("bacninh", "Bắc Ninh", "Miền Bắc", 21.1861, 106.0763),
        VietnamCity("namdinh", "Nam Định", "Miền Bắc", 20.4388, 106.1621),
        VietnamCity("thainguyen", "Thái Nguyên", "Miền Bắc", 21.5942, 105.8482),
        VietnamCity("ninhbinh", "Ninh Bình", "Miền Bắc", 20.2506, 105.9745),

        // Miền Trung & Tây Nguyên
        VietnamCity("danang", "Đà Nẵng", "Miền Trung", 16.0544, 108.2022),
        VietnamCity("hue", "Huế", "Miền Trung", 16.4637, 107.5909),
        VietnamCity("vinh", "Vinh (Nghệ An)", "Miền Trung", 18.6796, 105.6813),
        VietnamCity("thanhhoa", "Thanh Hóa", "Miền Trung", 19.8067, 105.7852),
        VietnamCity("hoian", "Hội An (Quảng Nam)", "Miền Trung", 15.8801, 108.3380),
        VietnamCity("quynhon", "Quy Nhơn (Bình Định)", "Miền Trung", 13.7830, 109.2197),
        VietnamCity("nhatrang", "Nha Trang (Khánh Hòa)", "Miền Trung", 12.2388, 109.1967),
        VietnamCity("dalat", "Đà Lạt (Lâm Đồng)", "Tây Nguyên", 11.9404, 108.4583),
        VietnamCity("buonmathuot", "Buôn Ma Thuột (Đắk Lắk)", "Tây Nguyên", 12.6667, 108.0500),
        VietnamCity("pleiku", "Pleiku (Gia Lai)", "Tây Nguyên", 13.9833, 108.0000),

        // Miền Nam
        VietnamCity("hcmc", "TP. Hồ Chí Minh", "Miền Nam", 10.8231, 106.6297),
        VietnamCity("cantho", "Cần Thơ", "Miền Nam", 10.0452, 105.7469),
        VietnamCity("vungtau", "Vũng Tàu", "Miền Nam", 10.3460, 107.0843),
        VietnamCity("bienhoa", "Biên Hòa (Đồng Nai)", "Miền Nam", 10.9574, 106.8427),
        VietnamCity("thudaumot", "Thủ Dầu Một (Bình Dương)", "Miền Nam", 10.9804, 106.6519),
        VietnamCity("phuquoc", "Phú Quốc (Kiên Giang)", "Miền Nam", 10.2289, 103.9572)
    )

    fun findById(cityId: String): VietnamCity {
        return CITIES.firstOrNull { it.id == cityId } ?: CITIES.first()
    }

    /**
     * Finds the nearest Vietnamese city to a given lat/lon for display naming when coarse location is used.
     */
    fun findNearestCity(lat: Double, lon: Double): VietnamCity {
        return CITIES.minByOrNull { city ->
            val dLat = city.latitude - lat
            val dLon = city.longitude - lon
            dLat * dLat + dLon * dLon
        } ?: CITIES.first()
    }
}
