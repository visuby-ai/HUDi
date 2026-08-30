# HUD UI (สำหรับ Forge 1.20.1 + ParCool)

มอดนี้ทำ HUD วงกลม 3 วง มุมล่างซ้ายแบบในรูปตัวอย่าง:

- **HP** (วงแดง หัวใจ + ตัวหนังสือ "HP") — มีวงแหวนบางๆ ล้อมรอบด้านนอกแสดงค่า **เกราะ (armor)**
- **Food** (วงฟ้า ไอคอนขาไก่/อาหาร) — ดึงค่าจากความหิวปกติของเกม
- **Stamina** (วงเขียว ไอคอนสายฟ้า) — **ดึงค่าจาก ParCool โดยตรง** ผ่าน API `com.alrex.parcool.api.Stamina` วงจะเป็นสีเทาเมื่อ exhausted

และมีการ:
- ปิดหัวใจ/เกราะ/ความหิว/EXP bar ของวานิลลาทั้งหมด (ใช้ของมอดนี้แทน)
- ปิด HUD stamina ของ ParCool เอง (ยกเลิกการ render ของ overlay ทุกตัวที่อยู่ใน namespace `parcool`) กันซ้อนกับวงสเตมิน่าของเรา
- เปลี่ยนช่องไอเทม (hotbar) เป็นแบบ minimal สีดำโปร่งแสง มุมตัด ไม่มีลายไม้ ช่องที่เลือกอยู่จะมีขอบสว่างบางๆ

## ⚠️ เหตุผลที่ UI ไม่ทำงาน (build ก่อนหน้า)

zip ที่อัปโหลดมาขาดไฟล์สำคัญไปทั้งหมด — ไม่ใช่บั๊กเล็กๆ:

- **ไม่มี `HudUiMod.java`** — คลาสหลัก `@Mod` ของมอด → Forge ไม่รู้จักมอดนี้เลย
- **ไม่มี `ClientModEvents.java`** — ตัวลงทะเบียน overlay → ต่อให้คอมไพล์ผ่าน ก็ไม่มีอะไรถูกสั่ง render
- **ไม่มี `ForgeClientEvents.java`** — ตัวปิด HUD วานิลลา/ของ ParCool
- **ไม่มี `HudShapes.java`** — ฟังก์ชันวาดรูปทรงที่ overlay ทั้งสองต้องเรียกใช้
- **ไม่มี `ParCoolStaminaAccess.java`**
- **ไม่มี `src/main/resources/META-INF/mods.toml`** — ไฟล์ manifest ที่ Forge ใช้ตรวจว่านี่คือมอด ถ้าไม่มีไฟล์นี้ Forge จะไม่โหลดมอดเลย
- ไฟล์ 2 ไฟล์ที่เหลืออยู่ (`VitalsHudOverlay.java`, `MinimalHotbarOverlay.java`) ก็เป็นแค่ code ค้าง/โน้ตแพตช์ที่ยังไม่ได้ implement จริง (คำนวณค่าแต่ไม่วาดอะไร / ไม่มี class ด้วยซ้ำ)

**สร้างไฟล์เหล่านี้ใหม่ทั้งหมดแล้วในโปรเจกต์นี้** พร้อม implement การวาดวงแหวน/hotbar จริง (ใช้ vertex buffer วาดวงแหวน ไม่ได้ใช้ texture รูปภาพ เพื่อลดความเสี่ยงเรื่อง asset หาย) และตรวจ API ของ ParCool จาก jar ที่แนบมาโดยตรง (`Stamina.get()`, `getValue()`, `getMaxValue()`, `isExhausted()`) เพื่อให้ compat class ตรงกับเวอร์ชันที่ใช้จริง

## โครงสร้างโปรเจกต์
```
mod/
├── build.gradle
├── gradle.properties         (mod_id = hudui)
├── libs/ParCool-1.20.1-3.4.3.3.jar   (ใช้ตอน compile เท่านั้น)
├── src/main/resources/
│   ├── META-INF/mods.toml    (ใหม่ — จำเป็นให้ Forge โหลดมอด)
│   └── pack.mcmeta           (ใหม่)
└── src/main/java/com/hudui/
    ├── HudUiMod.java                          (ใหม่ — main class)
    ├── client/
    │   ├── ClientModEvents.java               (ใหม่ — ลงทะเบียน overlay)
    │   ├── ForgeClientEvents.java              (ใหม่ — ปิด overlay วานิลลา + ของ ParCool)
    │   ├── overlay/VitalsHudOverlay.java       (เขียนใหม่ทั้งหมด — วงกลม HP/Armor/Food/Stamina)
    │   ├── overlay/MinimalHotbarOverlay.java   (เขียนใหม่ทั้งหมด — hotbar สีดำ minimal)
    │   └── render/HudShapes.java               (ใหม่ — วาดวงแหวน/สี่เหลี่ยมมุมตัดด้วย vertex buffer)
    └── compat/ParCoolStaminaAccess.java        (ใหม่ — wrapper API ของ ParCool)
```

## วิธี build

ต้องใช้เครื่องที่ต่อเน็ตได้ (ต้องโหลด Minecraft/Forge จาก maven ของ Forge เอง ซึ่งรันในแซนด์บ็อกซ์นี้ไม่ได้เพราะ network ถูกจำกัดโดเมน — **โค้ดชุดนี้ยังไม่เคยถูกคอมไพล์ทดสอบจริง** เขียนตาม Forge 1.20.1 API ที่ตรวจสอบแล้ว แต่ควร build แล้วดู error log จริงอีกทีถ้ามีปัญหา):

```bash
cd mod
./gradlew build
```

ไฟล์ jar ที่ได้จะอยู่ที่ `build/libs/hudui-1.0.0.jar`
เอาไปวางใน `mods/` คู่กับ `ParCool-1.20.1-3.4.3.3.jar` (มอดนี้ประกาศ **ต้องมี ParCool ติดตั้งด้วยเสมอ** ใน mods.toml ไม่งั้นเกมจะไม่ยอมโหลด)

ครั้งแรกที่รัน `./gradlew` จะโหลดไฟล์เยอะพอสมควร (decompile Minecraft) รอสักครู่ได้เลย

## จุดที่ปรับแต่งง่ายๆ ถ้าอยากขยับตำแหน่ง/สี
- `VitalsHudOverlay.java` — ค่าคงที่ด้านบนของคลาส (`RADIUS`, `MARGIN_LEFT`, `MARGIN_BOTTOM`, `GAUGE_GAP`, สีต่างๆ)
- `MinimalHotbarOverlay.java` — `SLOT_SIZE`, `SLOT_GAP`, สี `SLOT_BG` / `SELECTED_BG` / `SELECTED_BORDER`

## หมายเหตุ
- ไอคอน HP/food/stamina เป็นรูปทรงเวกเตอร์ที่วาดสดด้วยโค้ด (สี่เหลี่ยมข้าวหลามตัด/วงกลม/สายฟ้า) ไม่ใช้ไฟล์ png ถ้าอยากเปลี่ยนเป็นไอคอนจริงทีหลัง บอกมาได้ จะเพิ่มระบบโหลด texture ให้
- ถ้า build แล้วยัง crash หรือ HUD ไม่ขึ้น ส่ง `run/logs/latest.log` (หรือ `crash-reports/` ถ้า crash) มาดูได้เลย จะวินิจฉัยจาก log ตรงๆ แม่นกว่าเดา
