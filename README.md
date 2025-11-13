✨ Vampire Survivors Like – Android Mini Game
Android SurfaceView + Kotlin으로 구현된 간단한 뱀파이어 서바이버류 미니 게임입니다.
게임 시작 시 두 가지 무기 중 하나를 선택하고, 가상 조이스틱으로 이동하면서 자동 공격으로 몰려오는 적들을 처치합니다.
🎮 게임 특징
🔫 4가지 무기 타입
모든 무기는 Weapon 인터페이스를 기반으로 독립적인 공격 방식과 업그레이드 구조를 가집니다.
무기	특징
Sword (검)	플레이어 주변을 회전하며 공격. 실제 검 모양으로 렌더링. 업그레이드 시 개수 증가 및 크기 확대
Axe (도끼)	큰 원 궤도로 휘둘러 광역 타격, 넉백(konckback) + 생명력 흡수 10%
Bow (활)	최근접 적을 기준으로 부채꼴 3연발. 치명타 확률, 배율 증가. 관통 가능
Talisman (부적)	강력한 자동 추적. 빗나가도 궤도를 틀어 반드시 적을 명중하는 유도 투사체
🕹️ 조작 방식
화면 왼쪽 아래 고정 조이스틱으로 이동
공격은 자동 처리
적은 화면 네 방향에서 지속적으로 스폰되며 플레이어를 추적
📌 주요 구성 요소
✔ 프로젝트 구조
/GameView.kt              — 게임 루프, 화면 렌더링, 무기 선택 UI
/MainActivity.kt          — 앱 진입점
/player/Player.kt         — 플레이어 이동, 체력 시스템
/input/Joystick.kt        — 가상 조이스틱
/Enemy.kt                 — 적 이동, 피해 처리, 넉백
/weapons/Weapon.kt        — 무기 인터페이스
/weapons/Sword.kt         — 검
/weapons/Axe.kt           — 도끼
/weapons/Bow.kt           — 활
/weapons/Talisman.kt      — 부적(강력한 유도 시스템)
/weapons/WeaponFactory.kt — 무기 생성 공장
