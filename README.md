# ♟️ Java Chess (LibGDX Based)

This project is a **2D Chess game** developed using the **Java programming language** and the **LibGDX framework**. It features a comprehensive implementation of international chess rules alongside an **AI engine** optimized with a **Transposition Table (TT)** to enhance the experience for amateur players.

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

## 🌍 Language Versions

* English | Français | 中文

---

## English

### 📌 Overview

This is a **2D Chess game** developed with the **Java language** and the **LibGDX framework**. It supports **Local PvP** and **VS Computer** modes, featuring a customized AI and professional **Fischer Clock** timing.

### 🧠 Data Structures & AI Logic (60+ Methods)

* **Search Optimization (From Minimax to TT)**:
A standard 5-depth Minimax search required evaluating **~1.5 million nodes**.
* **The Solution**: Implemented **Zobrist Hashing** and a **Transposition Table (TT)**.
* **The Result**: The TT stores **Exact Values, Lower Bounds, and Upper Bounds**, enabling smooth 5-depth search by eliminating redundant calculations.


* **Move Ordering (MVV-LVA)**:
Prioritizes captures based on the **"Most Valuable Victim - Least Valuable Attacker"** heuristic (e.g., Queen captures Pawn > Pawn captures Queen) to maximize **Alpha-Beta pruning** efficiency.
* **Evaluation Engine**:
* **PST (Piece-Square Tables)**: Assigns positional bonuses based on master experience.
* **Endgame Logic**: When few pieces remain, the AI encourages **King activity and centralization**.
* **Automatic Promotion**: The AI automatically promotes pawns to Queens to streamline gameplay.



---

## Français

### 📌 Aperçu

Ce projet est un **jeu d'échecs 2D** développé avec le **langage Java** et le **framework LibGDX**. Il s'adresse aux amateurs et propose des modes **PvP local** et **IA vs Humain**, intégrant un système de chronométrage professionnel **Fischer**.

### 🧠 IA et Logique Algorithmique (Détails Techniques)

* **Optimisation de la recherche (TT & Zobrist)** :
Initialement, une recherche Minimax de profondeur 5 évaluait **1,5 million de nœuds**, causant des latences.
* **Solution** : Utilisation du **Hachage Zobrist** et d'une **Table de Transposition (TT)**.
* **Résultat** : La table stocke les **Valeurs Exactes, Bornes Inférieures et Supérieures**, rendant la recherche fluide.


* **Ordonnancement des coups (MVV-LVA)** :
Le moteur priorise les captures selon la logique **"Victime la plus précieuse - Agresseur le moins précieux"** (ex: Dame prend Pion > Pion prend Dame) pour optimiser l'élagage **Alpha-Bêta**.
* **Moteur d'Évaluation** :
* **PST (Piece-Square Tables)** : Attribution de bonus de position pour chaque pièce.
* **Logique de Finale** : En fin de partie, l'IA favorise la **centralisation et l'activité du Roi**.
* **Promotion Automatique** : L'IA choisit automatiquement la Reine lors de la promotion pour maintenir le rythme du jeu.



---

## 中文

### 📌 项目简介

本项目是采用 **Java 语言**与 **LibGDX 框架**开发打造的 **2D 国际象棋游戏**。支持**本地双人对战**与**人机对战**，专为**业余爱好者**设计，提供了流畅的博弈体验与三套视觉皮肤。

### 🧠 AI 与核心算法 (60 余个核心方法)

* **搜索优化 (从纯极小化极大到置换表)**:
最初 5 层深度的 Minimax 搜索需计算约 **150 万个节点**，导致卡顿。
* **解决方案**: 引入 **Zobrist Hashing** 与 **置换表 (Transposition Table)**。
* **优化结果**: 置换表通过存储**精确值、下界和上界**，彻底消除了重复计算，使搜索达到毫秒级响应。


* **走法排序 (MVV-LVA)**:
基于“**最有价值的受害者 - 最无价值的攻击者**”原则优先考虑吃子（如：后吃兵 > 兵吃后），显著提高 **Alpha-Beta 剪枝** 效率。
* **评估引擎**:
* **PST (棋子位置表)**: 根据棋子在不同阶段的位置给予评分加成。
* **残局逻辑**: 棋子较少时，算法会鼓励**王的活跃性与中心化**。
* **自动升变**: AI 回合触发兵升变时自动选择皇后。

