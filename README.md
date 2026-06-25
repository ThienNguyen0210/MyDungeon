# 🏰 MyDungeon

**MyDungeon** là plugin Minecraft (Paper/Spigot) tạo hệ thống **dungeon instance theo Party** — mỗi nhóm người chơi khi vào dungeon sẽ được cấp một **world tạm thời riêng biệt**, trải qua nhiều **stage** (giết quái MythicMobs, tới điểm chỉ định...), với độ khó tùy chỉnh, giới hạn thời gian, số mạng (lives), checkpoint, và phần thưởng theo sát thương gây ra.

---

## ✨ Tính năng chính

- **World instance riêng cho từng Party** — mỗi lần vào dungeon, plugin tạo một thế giới tạm (`temp_<dungeonId>_...`) chỉ dành cho nhóm đó, tránh đụng độ giữa các party.
- **Hệ thống Party** — tạo nhóm, mời thành viên, đánh dấu sẵn sàng (ready check) trước khi cả nhóm cùng vào dungeon.
- **Nhiều Stage nối tiếp** — mỗi dungeon gồm nhiều giai đoạn: giết quái MythicMobs (đơn mục tiêu hoặc nhiều mục tiêu cùng lúc), tới vị trí chỉ định, kèm title/subtitle, message, và lệnh tùy chỉnh khi vào stage.
- **Tích hợp MythicMobs** — spawn quái theo ID MythicMobs, hỗ trợ animation spawn (tức thì hoặc theo từng đợt/batch), particle + sound khi mob xuất hiện, mob tự AI quay về vị trí gốc.
- **Hệ thống độ khó (Difficulty)** — nhân hệ số máu/damage/tốc độ của quái theo độ khó được chọn cho từng world.
- **Giới hạn thời gian (Time Limit)** — hiện boss bar đếm ngược, tự động xử lý khi hết giờ.
- **Hệ thống mạng sống (Lives)** — giới hạn số lần chết tối đa trước khi dungeon thất bại.
- **Checkpoint** — lưu điểm hồi sinh trong dungeon, không phải quay lại từ đầu mỗi lần chết.
- **Bảng xếp hạng sát thương & phần thưởng theo damage** — tổng kết top 5 người gây sát thương nhiều nhất, phát thưởng theo các mức (tier) sát thương đạt được.
- **Cơ chế Tower / Cooldown theo tầng** — một số dungeon dạng "tháp" có thể giới hạn thời gian chờ giữa các lần đánh lại cùng một tầng.
- **Block Group (break-group / place-group)** — kích hoạt phá hoặc đặt một nhóm block tại các vị trí định trước, kèm hiệu ứng particle/sound — dùng cho hiệu ứng cơ chế (mở cửa, sập sàn, dựng tường...).
- **Hệ thống item yêu cầu/phần thưởng dùng chung (`require.yml`)** — lưu item dạng Base64 theo ID số, dùng làm điều kiện vào dungeon hoặc phần thưởng stage.
- **Giới hạn lệnh trong dungeon** — chặn danh sách lệnh cấu hình sẵn khi người chơi đang trong instance.
- ** còn nhiều ....

---

## 📋 Yêu cầu

- Server Paper **1.19+**
- [MythicMobs](https://www.spigotmc.org/resources/mythicmobs.5702/) — **bắt buộc**, dùng để spawn quái cho mọi stage dạng `KILL_MYTHIC_MOB`

---

## 📦 Cài đặt

1. Thả file `.jar` vào `plugins/`.
2. Khởi động lại server. Plugin sẽ tự tạo:
   - `plugins/MyDungeon/config.yml` — cấu hình chung (độ khó, animation spawn quái, giới hạn lệnh...)
   - `plugins/MyDungeon/messages.yml` — toàn bộ message hiển thị cho người chơi
   - `plugins/MyDungeon/Dungeons/` — chứa file cấu hình từng dungeon (`<dungeonId>.yml`)
   - `plugins/MyDungeon/require.yml` — database item dùng chung (Base64)
   - `plugins/MyDungeon/dungeon-timers.yml` — lưu trạng thái timer

---

## ⚙️ Cấu hình một dungeon (`Dungeons/<id>.yml`)

```yaml
max-lives: 3                      # số lần chết tối đa trước khi thua
message-first: "&a&lChào mừng đến với hầm ngục!"
spawn-location: "world_dungeons,100.5,64,200.5,0,0"
send-damage-summary: true
quit-time: 5                       # giây trước khi đẩy người chơi ra sau khi thắng

time-limit:
  enabled: true
  seconds: 300

damage-rewards:
  enabled: true
  tiers:
    EASY:
      - "500:100:1"     # cần >=500 dame, 100% rớt, item ID 1 (trong require.yml)
      - "2000:50:2"      # cần >=2000 dame, 50% rớt, item ID 2
    DEFAULT:
      - "1000:30:1"

win-commands:
  - "give %player% diamond 1"

stages:
  1:
    type: "KILL_MYTHIC_MOB"
    target: "SkeletonBoss"           # mob ID đơn (nếu không dùng targets)
    location: "100,64,200"            # vị trí spawn (x,y,z)
    goal: 1
    delay: 3                          # giây trễ trước khi mob xuất hiện
    name: "Giai đoạn 1: Lính gác"
    message: "&eHãy đánh bại Lính Gác!"
    title: "&c&lSTAGE 1"
    subtitle: "&7Giết Lính Gác để tiếp tục"
    set-checkpoint: true
    ai: true
    ai-target: null
    reward-item-id: 1
    commands:
      - "say Stage 1 bắt đầu!"
  2:
    type: "KILL_MYTHIC_MOB"
    goal: 0                            # sẽ tự tính tổng từ targets bên dưới
    targets:
      - mob: "Goblin"
        location: "105,64,205"
        goal: 3
      - mob: "GoblinArcher"
        location: "110,64,205"
        goal: 2
    name: "Giai đoạn 2: Bầy Goblin"

# Block group — kích hoạt qua code/lệnh nội bộ, không tự động chạy
break-groups:
  door_1:
    - "120,64,210"
    - "121,64,210"
place-groups:
  wall_collapse:
    - "120,64,210,COBBLESTONE"
    - "121,64,210,COBBLESTONE"

# Dùng cho dungeon dạng tháp (tower)
is-tower-stage: false
stage-number: 1
```

### Giải thích các loại Stage

| Field | Ý nghĩa |
|---|---|
| `type: KILL_MYTHIC_MOB` | Stage yêu cầu giết quái MythicMobs mới qua được |
| `target` + `location` + `goal` | Dùng khi stage chỉ có **1 loại quái** duy nhất |
| `targets` (list) | Dùng khi stage có **nhiều loại quái** cùng lúc, mỗi target có `mob`, `location`, `goal` riêng — tổng `goal` toàn stage được tự tính cộng dồn |
| `delay` | Số giây trễ trước khi mob xuất hiện sau khi vào stage |
| `set-checkpoint: true` | Lưu vị trí stage này làm điểm hồi sinh nếu người chơi chết |
| `ai: true` | Quái sau khi bị kéo ra xa sẽ tự động quay về vị trí gốc |
| `reward-item-id` | ID item (định nghĩa trong `require.yml`) phát cho cả nhóm khi hoàn thành stage |

---

## ⚙️ Cấu hình chung (`config.yml`)

```yaml
mob-spawn-animation:
  mode: "instant"            # instant | batch
  delay-ticks: 20             # khoảng cách giữa các đợt (nếu mode = batch)
  batch-size: 5               # số quái spawn mỗi đợt
  random-radius: 5.0          # bán kính random vị trí spawn quanh điểm gốc
  spawn-particle:
    enabled: true
    type: SMOKE_NORMAL
    count: 15
    offset: 0.4
    speed: 0.1
  spawn-sound:
    enabled: true
    sound: ENTITY_ZOMBIE_INFECT
    volume: 0.8
    pitch: 1.0

difficulty-settings:
  EASY:
    health-multiplier: 1.0
    damage-multiplier: 1.0
    speed-multiplier: 1.0
  HARD:
    health-multiplier: 2.0
    damage-multiplier: 1.5
    speed-multiplier: 1.2

dungeon-command-restriction:
  enabled: true
  blocked-commands:
    - "tp"
    - "home"

tower-settings:
  reset-times:
    1: 86400     # tầng 1: cooldown 24 giờ (giây) trước khi đánh lại
    2: 172800    # tầng 2: cooldown 48 giờ
```

> Quái được spawn ở chế độ `batch` sẽ được tìm vị trí "an toàn" tự động xung quanh điểm gốc (không kẹt tường, có mặt đất, không spawn trong nước/lửa) trong bán kính `random-radius`.

---

## 🗂️ Item dùng chung (`require.yml`)

Lưu item thật (kèm enchant, lore, NBT...) dưới dạng Base64, tham chiếu bằng ID số nguyên — dùng làm phần thưởng stage hoặc điều kiện vào dungeon.

```yaml
items:
  1:
    base64: "<chuỗi base64 của item>"
  2:
    base64: "<chuỗi base64 của item>"
```

> Plugin tự lưu/đọc qua các hàm nội bộ (`saveRequireToDB`, `removeRequireFromDB`) — không cần chỉnh tay trừ khi bạn đang import/export dữ liệu giữa các server.

---

## 👥 Hệ thống Party

Mỗi Party gồm:
- 1 **leader** (có thể chuyển nhượng)
- Danh sách **members**
- Trạng thái **ready check** — mỗi thành viên cần đánh dấu sẵn sàng, dungeon chỉ bắt đầu khi `isAllReady()` trả về `true` (tất cả thành viên đều sẵn sàng)

> Lưu ý: file `Party.java` chỉ định nghĩa cấu trúc dữ liệu — lệnh thực tế để tạo/mời/sẵn sàng party (`/party create`, `/party invite`...) nằm ở các class command khác chưa có trong tài liệu này.

---

## ⚰️ Mạng sống, Checkpoint & Thất bại

- Mỗi dungeon có `max-lives` (mặc định 3) — vượt quá số lần chết cho phép sẽ khiến dungeon **thất bại** (`failDungeon`): toàn bộ người chơi trong world bị chuyển sang Spectator, world được dọn dẹp và đóng.
- `set-checkpoint: true` trên 1 stage sẽ lưu vị trí đó làm điểm hồi sinh — người chơi chết sau đó sẽ hồi sinh tại checkpoint gần nhất, không phải về stage 1.
- Dungeon được coi là còn hoạt động nếu **ít nhất 1 thành viên** chưa rơi vào Spectator (`isAnyMemberAlive`).

---

## 🏆 Khi hoàn thành dungeon (Win)

Khi qua hết toàn bộ stage, `winDungeon()` được gọi:
1. Đánh dấu toàn bộ người chơi trong world là `DungeonState.END`.
2. Gửi **bảng xếp hạng sát thương** (top 5) nếu `send-damage-summary: true`.
3. Phát **phần thưởng theo tier sát thương** (`damage-rewards`) — random theo % tỉ lệ đã cấu hình, riêng theo từng độ khó hoặc dùng tier `DEFAULT` nếu độ khó đó chưa cấu hình riêng.
4. Chạy các lệnh trong `win-commands`.
5. Sau `quit-time` giây, người chơi được đưa ra khỏi world tạm và world bị dọn dẹp hoàn toàn.

---

## 🏗️ Dungeon dạng Tháp (Tower)

Nếu `is-tower-stage: true`, dungeon đó được coi là 1 tầng tháp với `stage-number` riêng:
- Sau khi thắng, người chơi phải chờ một khoảng cooldown (cấu hình tại `tower-settings.reset-times.<tầng>`, đơn vị giây) mới được đánh lại tầng đó.
- `canEnterTower()` tự kiểm tra điều kiện này trước khi cho vào, báo thời gian còn lại nếu chưa đủ.

---

## ⚠️ Giới hạn lệnh trong Dungeon

Khi `dungeon-command-restriction.enabled: true`, các lệnh trong `blocked-commands` sẽ bị chặn đối với người chơi đang ở trong world instance — tránh việc dùng `/tp`, `/home`... để thoát dungeon bất hợp lệ.

---

## ❓ Lưu ý kỹ thuật

- World instance dùng quy ước tên `temp_<dungeonId>_...` — toàn bộ logic nhận diện "đang trong dungeon" dựa vào tiền tố này.
- Mob được spawn qua API của MythicMobs (`MythicBukkit.inst().getMobManager().spawnMob(...)`) — đảm bảo các mob ID trong `target`/`targets` đã được định nghĩa hợp lệ trong MythicMobs trước khi dùng.
- Hệ số độ khó chỉ áp dụng cho entity spawn trong world có tên bắt đầu bằng `temp_` — không ảnh hưởng tới mob ngoài dungeon.
- `getMaxLives()` có cache theo `dungeonId` — nếu sửa `max-lives` trong file dungeon khi server đang chạy, cần đảm bảo cache được làm mới (qua lệnh reload tương ứng của plugin) để áp dụng giá trị mới.

---

## 📜 Bản quyền

Tác giả: **ThienNguyen**
