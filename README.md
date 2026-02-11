# ♟️ Java Chess (LibGDX Based)

## 📁 Project Structure 

```text
Chess/
├── Assets/              # Piece textures, board skins, and UI assets
├── src/                 # Source code
│   ├── Main.java        # Entry point: Lwjgl3 config, VSync, 60FPS
│   ├── Chess.java       # UI Controller: Main menu, Day/Night/Dusk skins
│   ├── GameBoard.java   # Rule engine: Fischer Clock, Raycasting, FSM
│   └── AI.java          # AI Brain: Zobrist TT, MVV-LVA, PST evaluation
└── External Libraries   # LibGDX (gdx-liftoff), OpenJDK 25

```
<table>
  <tr>
    <td align="center">
      <h3>Local PvP</h3>
      <img src="https://github.com/user-attachments/assets/479192ab-1d65-4236-b997-42823bc2dd30" width="100%"/>
    </td>
    <td align="center">
      <h3>VS AI</h3>
      <img src="https://github.com/user-attachments/assets/c30871f1-53ca-45b3-94fb-1cc148397bbd" width="100%"/>
    </td>
  </tr>
</table>

---
## About / À Propos / 关于

This project is a **2D Chess game** developed using the **Java programming language** and the **LibGDX framework**. It features a comprehensive implementation of international chess rules alongside a **custom-built AI engine**. While not a "from-scratch" research project, this engine was personally developed and optimized with a **Transposition Table (TT)** to enhance the experience for amateur players.

Ce projet est un **jeu d'échecs 2D** développé avec le **langage Java** et le **framework LibGDX**. Il propose une implémentation complète des règles du jeu d'échecs international, ainsi qu'un **moteur d'IA personnalisé**. Bien qu'il ne s'agisse pas d'une recherche fondamentale à partir de zéro, ce moteur a été développé et optimisé individuellement avec une **Table de Transposition (TT)** pour améliorer l'expérience des joueurs amateurs.

这是一个使用 **Java 语言**和 **LibGDX 框架**开发的 **2D 国际象棋游戏**。项目完整实现了国际象棋的各项规则，并搭载了一个**自主编写的 AI 引擎**。该引擎并非完全脱离现有理论的底层研发，而是在现有博弈算法基础上进行个人实现，并通过**置换表 (TT)** 进行了针对性优化，旨在为业余玩家提供更具挑战性的游戏体验。

---

## ♟️ Technical Details 

### • Main.java (Startup & Configuration)

* **Bootstrap**: Sets up the rendering environment with **Vsync** and 60FPS target for smooth visual output.
* **GPU Acceleration**: Leverages the LibGDX framework to utilize hardware acceleration for efficient graphics redrawing.

### • AI.java (Decision Engine & Performance Optimization)

* **Performance Crisis**: The initial **Minimax** algorithm (with Alpha-Beta pruning) at depth 5 required evaluating **1.5 million+ nodes**. Even on a mid-to-high-end PC (**RTX 3050Ti + Ryzen 5600H**), this caused severe lag, rendering the game unplayable.
* **Self-Taught Optimization (Transposition Table)**: To overcome this hardware bottleneck, I **self-studied and implemented** advanced optimization techniques:
* **Zobrist Hashing**: Generates 64-bit unique keys for board states to track repeated positions.
* **TT Implementation**: By storing exact values and bounds (Lower/Upper), the system eliminates redundant searches, successfully reducing search latency from seconds to **milliseconds**.


* **Heuristics & Evaluation**:
* **Move Ordering (MVV-LVA)**: Prioritizes high-value captures to maximize pruning efficiency.
* **PST Evaluation**: Uses **Piece-Square Tables** for phase-specific positional scoring.
* **Endgame & Promotion**: Optimizes King centralization in endgames and implements **Automatic Promotion** for AI efficiency.


* **Human-like UX**: Features an **asynchronous 2-5s randomized delay** to simulate human deliberation.

### • Chess.java (State Control & UI Engine)

* **State Machine**: Orchestrates transitions between **PvP** and **PvE** modes while managing the overall game lifecycle.
* **Responsive UI System**: A custom-coded **Scene2D** layout engine that enables **dynamic scaling** across varying window resolutions.
* **Texture Cache**: Utilizes a `HashMap` based texture pooling system to minimize VRAM overhead.

### • GameBoard.java (Rules Engine & Logic Validation)

* **Move Validation**: Full implementation of **Castling**, **En Passant**, and **Pawn Promotion**.
* **Backtracking Simulation**: Features a `simulateAndCheck` method that pre-calculates moves in a memory buffer to prevent illegal moves (e.g., self-check).
* **Fischer Clock**: Professional timing system with incremental time bonuses per move.

---

## ♟️ Détails Techniques 

### • Main.java (Démarrage et Configuration)

* **Initialisation**: Configuration de l'OpenGL avec **Vsync** et 60 FPS pour une fluidité d'affichage optimale.
* **Accélération Matérielle**: Utilise les ressources GPU via LibGDX pour un rendu fluide.

### • AI.java (Moteur de Décision et Optimisation)

* **Crise de Performance**: L'algorithme **Minimax** initial (profondeur 5) évaluait **1,5 million de nœuds**. Même sur une configuration puissante (**RTX 3050Ti + Ryzen 5600H**), cela provoquait des ralentissements majeurs.
* **Auto-apprentissage (Table de Transposition)** : Pour résoudre ce goulot d'étranglement, j'ai **appris et implémenté de manière autonome** des techniques avancées :
* **Hachage Zobrist**: Génération de clés 64 bits uniques pour chaque état.
* **Optimisation TT**: Stocke les valeurs et les bornes pour supprimer les calculs redondants, réduisant le temps de réponse à quelques **millisecondes**.


* **Heuristiques d'Évaluation**:
* **MVV-LVA**: Priorise les captures pour optimiser l'élagage.
* **Tables PST**: Attribution de scores selon la position des pièces.
* **Logique de Finale**: Optimise la centralisation du Roi et gère la **promotion automatique**.


* **Interaction Asynchrone**: Intègre un **délai aléatoire de 2 à 5s** pour simuler la réflexion humaine.

### • Chess.java (Contrôle d'État et Moteur UI)

* **Gestionnaire d'État**: Contrôle les transitions entre les modes **PvP** et **PvE**.
* **Système UI Réactif**: Interface **Scene2D** permettant une **adaptation dynamique** à toutes les résolutions.
* **Gestion des Textures**: Cache de textures via `HashMap` pour optimiser la mémoire vidéo.

### • GameBoard.java (Moteur de Règles et Validation Logique)

* **Moteur de Règles**: Implémentation complète du **Roque**, de la **Prise en passant** et de la **Promotion**.
* **Validation par Simulation**: Utilise la méthode `simulateAndCheck` pour bloquer tout mouvement illégal.
* **Chronomètre Fischer**: Système de temps professionnel avec incréments par coup.

---

## 🛠️ 技术细节 

### • Main.java (启动与配置中心)

* **环境初始化**：负责底层框架配置，开启 **Vsync (垂直同步)** 并设定 60FPS 的目标帧率，确保游戏画面的丝滑程度。
* **硬件加速适配**：基于 LibGDX 引擎利用显卡 GPU 资源进行高效渲染，为后续的高频率画面重绘提供稳定的支撑。

### • AI.java (博弈引擎与高性能优化)

* **性能瓶颈实测**：初始版本采用基础 **Minimax** 算法配合 **Alpha-Beta 剪枝**。在 5 层深度搜索下需评估约 **150 万个节点**。实测证明，即便在配置为 **RTX 3050Ti + Ryzen 5600H** 的高性能笔记本上，依然会出现严重卡顿，无法正常对弈。
* **自学突破 (置换表技术)**：为了攻克硬件性能瓶颈，我**自学并引入**了置换表系统进行底层优化：
* **Zobrist Hashing**：引入哈希键值生成技术，为每个棋子与格点生成唯一的 64 位标识。
* **冗余消除与加速**：自主构建置换表存储已搜索过的**精确值 (Exact)**、**下界 (Lower Bound)** 和 **上界 (Upper Bound)**。通过查表机制替代重复搜索，成功将搜索响应从数秒卡顿优化至 **毫秒级瞬时响应**。


* **评估体系与启发式策略**：
* **走法排序 (MVV-LVA)**：基于“最有价值受害者 - 最无价值攻击者”原则优先搜索吃子走法，极大提升剪枝效率。
* **PST 分值评估 (Piece-Square Tables)**：参考成熟棋谱权重，为不同棋子在不同阶段（开局/中局/残局）的格点位置动态分配分值。
* **残局逻辑与自动升变**：针对残局强化“王”的中心化侵略性；当 AI 触发兵升变时，系统会自动执行升变皇后操作。


* **异步交互模拟**：设计了 **2-5 秒异步随机延迟落子**，模拟人类思考过程，增强博弈真实感。

### • Chess.java (状态控制与 UI 引擎)

* **状态机控制**：通过核心控制器管理 **PvP (本地对战)** 与 **PvE (人机对战)** 模式的平滑切换，维护游戏的完整生命周期。
* **响应式 UI 系统**：纯代码手搓调试 **Scene2D UI** 布局，通过对一百多个核心方法的封装，实现了从全屏模式到任意窗口比例的 **动态比例适配** 与自动重绘。
* **资源池化管理**：采用 `HashMap` 建立纹理缓存（Texture Cache），配合字符串拼接技术实现资源的动态按需加载，极大地优化了显存占用。

### • GameBoard.java (规则引擎与逻辑校验)

* **规则完整性**：精准还原 **王车易位 (Castling)**、**吃过路兵 (En Passant)** 及 **兵的升变 (Promotion)** 等全套国际象棋特殊规则。
* **模拟回溯验证**：内置 `simulateAndCheck` 方法。在任何棋子移动执行前，系统会在内存镜像中完成“虚拟推演”，通过回溯算法确保所有移动均符合安全规则，从底层拦截非法走法。
* **费舍尔计时系统**：实现了专业的 **Fischer Clock** 计时逻辑，支持每步补偿时间（Increment），确保比赛节奏的专业性。

