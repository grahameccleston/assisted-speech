package com.assisted.speech.data

object PhraseTreeData {

    data class PhraseNode(
        val id: String,
        val text: String,
        val iconEmoji: String = "💬",
        val children: List<PhraseNode> = emptyList(),
        val isCompleteSentence: Boolean = false
    )

    // Quick full sentences / responses that can be spoken directly or added
    val quickPhrases = listOf(
        PhraseNode("q_yes", "Yes", "✅", isCompleteSentence = true),
        PhraseNode("q_no", "No", "❌", isCompleteSentence = true),
        PhraseNode("q_please", "Please", "🙏", isCompleteSentence = true),
        PhraseNode("q_thank_you", "Thank you", "🙌", isCompleteSentence = true),
        PhraseNode("q_help", "I need help", "🆘", isCompleteSentence = true),
        PhraseNode("q_welcome", "You're welcome", "🤝", isCompleteSentence = true),
        PhraseNode("q_hello", "Hello", "👋", isCompleteSentence = true),
        PhraseNode("q_goodbye", "Goodbye", "👋", isCompleteSentence = true),
        PhraseNode("q_good_morning", "Good morning", "🌅", isCompleteSentence = true),
        PhraseNode("q_good_night", "Good night", "🌙", isCompleteSentence = true),
        PhraseNode("q_how_are_you", "How are you?", "❓", isCompleteSentence = true),
        PhraseNode("q_i_am_fine", "I am fine", "👍", isCompleteSentence = true),
        PhraseNode("q_excuse_me", "Excuse me", "🗣️", isCompleteSentence = true),
        PhraseNode("q_dont_know", "I don't know", "🤷", isCompleteSentence = true),
        PhraseNode("q_maybe_later", "Maybe later", "⏳", isCompleteSentence = true),
        PhraseNode("q_love_you", "I love you", "❤️", isCompleteSentence = true)
    )

    // Deep hierarchical grammatical tree
    // Tier 1 (Starters) -> Tier 2 (Actions/Verbs/Predicates) -> Tier 3 (Objects/Locations) -> Tier 4 (Details/Modifiers)
    val rootTreeNodes = listOf(
        // ==========================================
        // BRANCH 1: "Can you help me to..."
        // ==========================================
        PhraseNode(
            id = "b_help_to",
            text = "Can you help me to",
            iconEmoji = "🤝",
            children = listOf(
                PhraseNode(
                    id = "b_help_to_sit",
                    text = "sit up",
                    iconEmoji = "🪑",
                    children = listOf(
                        PhraseNode("h_sit_bed", "in bed", "🛏️", children = listOf(
                            PhraseNode("h_sit_bed_now", "right now", "⏱️"),
                            PhraseNode("h_sit_bed_please", "please", "🙏")
                        )),
                        PhraseNode("h_sit_chair", "in the chair", "🪑", children = listOf(
                            PhraseNode("h_sit_chair_now", "right now", "⏱️"),
                            PhraseNode("h_sit_chair_gently", "gently", "🤲")
                        )),
                        PhraseNode("h_sit_wheelchair", "in wheelchair", "🧑‍🦽")
                    )
                ),
                PhraseNode(
                    id = "b_help_to_lie",
                    text = "lie down",
                    iconEmoji = "🛏️",
                    children = listOf(
                        PhraseNode("h_lie_flat", "flat", "🛏️"),
                        PhraseNode("h_lie_side", "on my side", "🔄"),
                        PhraseNode("h_lie_back", "on my back", "🛌"),
                        PhraseNode("h_lie_pillow", "with extra pillow", "🛋️")
                    )
                ),
                PhraseNode(
                    id = "b_help_to_stand",
                    text = "stand up",
                    iconEmoji = "🧍",
                    children = listOf(
                        PhraseNode("h_std_slowly", "slowly", "🐢"),
                        PhraseNode("h_std_frame", "with my walking frame", "🦯"),
                        PhraseNode("h_std_now", "please", "🙏")
                    )
                ),
                PhraseNode(
                    id = "b_help_to_walk",
                    text = "walk to",
                    iconEmoji = "🚶",
                    children = listOf(
                        PhraseNode("h_walk_toilet", "the toilet", "🚻"),
                        PhraseNode("h_walk_bed", "my bed", "🛏️"),
                        PhraseNode("h_walk_outside", "outside", "🌳"),
                        PhraseNode("h_walk_chair", "the chair", "🪑")
                    )
                ),
                PhraseNode(
                    id = "b_help_to_reach",
                    text = "reach",
                    iconEmoji = "🤲",
                    children = listOf(
                        PhraseNode("h_rch_phone", "my phone", "📱"),
                        PhraseNode("h_rch_glasses", "my glasses", "👓"),
                        PhraseNode("h_rch_water", "my water", "💧"),
                        PhraseNode("h_rch_call", "the call button", "🚨")
                    )
                ),
                PhraseNode(
                    id = "b_help_to_put_on",
                    text = "put on",
                    iconEmoji = "👕",
                    children = listOf(
                        PhraseNode("h_po_glasses", "my glasses", "👓"),
                        PhraseNode("h_po_shoes", "my slippers", "👟"),
                        PhraseNode("h_po_jacket", "my jacket", "🧥"),
                        PhraseNode("h_po_blanket", "my blanket", "🛏️")
                    )
                ),
                PhraseNode(
                    id = "b_help_to_adjust",
                    text = "adjust",
                    iconEmoji = "🔄",
                    children = listOf(
                        PhraseNode("h_adj_pillows", "my pillows", "🛋️"),
                        PhraseNode("h_adj_bed", "the bed height", "🛏️"),
                        PhraseNode("h_adj_blanket", "my blanket", "🧣")
                    )
                )
            )
        ),

        // ==========================================
        // BRANCH 2: "I want to..."
        // ==========================================
        PhraseNode(
            id = "b_want_to",
            text = "I want to",
            iconEmoji = "👉",
            children = listOf(
                PhraseNode(
                    id = "w_go_to",
                    text = "go to",
                    iconEmoji = "🚶",
                    children = listOf(
                        PhraseNode("w_go_toilet", "the toilet", "🚻", children = listOf(
                            PhraseNode("w_go_toilet_now", "urgently", "⚡"),
                            PhraseNode("w_go_toilet_pls", "please", "🙏")
                        )),
                        PhraseNode("w_go_bed", "bed", "🛏️", children = listOf(
                            PhraseNode("w_go_bed_sleep", "to sleep", "😴"),
                            PhraseNode("w_go_bed_rest", "to rest", "🛋️")
                        )),
                        PhraseNode("w_go_outside", "outside", "🌳", children = listOf(
                            PhraseNode("w_go_out_air", "for fresh air", "🌬️"),
                            PhraseNode("w_go_out_walk", "for a walk", "🚶")
                        )),
                        PhraseNode("w_go_home", "home", "🏠")
                    )
                ),
                PhraseNode(
                    id = "w_drink",
                    text = "drink",
                    iconEmoji = "🥤",
                    children = listOf(
                        PhraseNode("w_drk_water", "some water", "💧", children = listOf(
                            PhraseNode("w_drk_cold", "cold", "🧊"),
                            PhraseNode("w_drk_warm", "warm", "☕")
                        )),
                        PhraseNode("w_drk_tea", "a cup of tea", "☕"),
                        PhraseNode("w_drk_juice", "some juice", "🧃")
                    )
                ),
                PhraseNode(
                    id = "w_eat",
                    text = "eat",
                    iconEmoji = "🍽️",
                    children = listOf(
                        PhraseNode("w_eat_food", "some food", "🍲"),
                        PhraseNode("w_eat_meal", "lunch", "🥪"),
                        PhraseNode("w_eat_snack", "a snack", "🍎"),
                        PhraseNode("w_eat_soup", "some soup", "🥣")
                    )
                ),
                PhraseNode(
                    id = "w_watch",
                    text = "watch",
                    iconEmoji = "📺",
                    children = listOf(
                        PhraseNode("w_wtch_tv", "the television", "📺"),
                        PhraseNode("w_wtch_news", "the news", "📰"),
                        PhraseNode("w_wtch_movie", "a movie", "🎬")
                    )
                ),
                PhraseNode(
                    id = "w_listen",
                    text = "listen to",
                    iconEmoji = "🎵",
                    children = listOf(
                        PhraseNode("w_lsn_music", "some music", "🎶"),
                        PhraseNode("w_lsn_radio", "the radio", "📻"),
                        PhraseNode("w_lsn_audio", "an audiobook", "🎧")
                    )
                ),
                PhraseNode(
                    id = "w_rest",
                    text = "rest",
                    iconEmoji = "😴",
                    children = listOf(
                        PhraseNode("w_rst_now", "now for a while", "⏱️"),
                        PhraseNode("w_rst_quiet", "in quiet", "🤫")
                    )
                )
            )
        ),

        // ==========================================
        // BRANCH 3: "I need..."
        // ==========================================
        PhraseNode(
            id = "b_need",
            text = "I need",
            iconEmoji = "🙋",
            children = listOf(
                PhraseNode(
                    id = "n_toilet",
                    text = "the toilet",
                    iconEmoji = "🚻",
                    children = listOf(
                        PhraseNode("n_t_now", "urgently", "⚡"),
                        PhraseNode("n_t_help", "with help", "🤝"),
                        PhraseNode("n_t_pls", "please", "🙏")
                    )
                ),
                PhraseNode(
                    id = "n_med",
                    text = "my medicine",
                    iconEmoji = "💊",
                    children = listOf(
                        PhraseNode("n_med_pain", "for pain", "🩹"),
                        PhraseNode("n_med_now", "right now", "⏱️"),
                        PhraseNode("n_med_water", "with a glass of water", "💧")
                    )
                ),
                PhraseNode(
                    id = "n_doc",
                    text = "to see",
                    iconEmoji = "🩺",
                    children = listOf(
                        PhraseNode("n_doc_doc", "the doctor", "👨‍⚕️"),
                        PhraseNode("n_doc_nurse", "the nurse", "👩‍⚕️"),
                        PhraseNode("n_doc_fam", "my family", "👨‍👩‍👧")
                    )
                ),
                PhraseNode(
                    id = "n_item",
                    text = "my",
                    iconEmoji = "🔍",
                    children = listOf(
                        PhraseNode("n_it_glasses", "glasses", "👓"),
                        PhraseNode("n_it_phone", "phone", "📱"),
                        PhraseNode("n_it_blanket", "blanket", "🛏️"),
                        PhraseNode("n_it_pillow", "pillow", "🛋️"),
                        PhraseNode("n_it_tissue", "tissue", "🤧")
                    )
                )
            )
        ),

        // ==========================================
        // BRANCH 4: "Can you please..."
        // ==========================================
        PhraseNode(
            id = "b_can_you_pls",
            text = "Can you please",
            iconEmoji = "🙏",
            children = listOf(
                PhraseNode(
                    id = "p_turn_on",
                    text = "turn on",
                    iconEmoji = "💡",
                    children = listOf(
                        PhraseNode("p_on_light", "the lights", "💡"),
                        PhraseNode("p_on_tv", "the TV", "📺"),
                        PhraseNode("p_on_fan", "the fan", "🌀"),
                        PhraseNode("p_on_heat", "the heating", "🔥")
                    )
                ),
                PhraseNode(
                    id = "p_turn_off",
                    text = "turn off",
                    iconEmoji = "🌑",
                    children = listOf(
                        PhraseNode("p_off_light", "the lights", "🌑"),
                        PhraseNode("p_off_tv", "the TV", "📺"),
                        PhraseNode("p_off_fan", "the fan", "🛑")
                    )
                ),
                PhraseNode(
                    id = "p_open",
                    text = "open",
                    iconEmoji = "🚪",
                    children = listOf(
                        PhraseNode("p_op_door", "the door", "🚪"),
                        PhraseNode("p_op_win", "the window", "🪟"),
                        PhraseNode("p_op_curt", "the curtains", "🪟")
                    )
                ),
                PhraseNode(
                    id = "p_close",
                    text = "close",
                    iconEmoji = "🚪",
                    children = listOf(
                        PhraseNode("p_cl_door", "the door", "🚪"),
                        PhraseNode("p_cl_win", "the window", "🪟"),
                        PhraseNode("p_cl_curt", "the curtains", "🪟")
                    )
                ),
                PhraseNode(
                    id = "p_call",
                    text = "call",
                    iconEmoji = "📞",
                    children = listOf(
                        PhraseNode("p_cl_nurse", "the nurse", "👩‍⚕️"),
                        PhraseNode("p_cl_doc", "the doctor", "👨‍⚕️"),
                        PhraseNode("p_cl_family", "my family", "👨‍👩‍👧")
                    )
                ),
                PhraseNode(
                    id = "p_give",
                    text = "give me",
                    iconEmoji = "🤲",
                    children = listOf(
                        PhraseNode("p_gv_water", "a drink of water", "💧"),
                        PhraseNode("p_gv_tissue", "a tissue", "🤧"),
                        PhraseNode("p_gv_moment", "a minute to rest", "⏳")
                    )
                )
            )
        ),

        // ==========================================
        // BRANCH 5: "I feel..."
        // ==========================================
        PhraseNode(
            id = "b_feel",
            text = "I feel",
            iconEmoji = "💭",
            children = listOf(
                PhraseNode(
                    id = "f_pain",
                    text = "pain in my",
                    iconEmoji = "🩹",
                    children = listOf(
                        PhraseNode("f_pn_head", "head", "🤕"),
                        PhraseNode("f_pn_back", "back", "🧍"),
                        PhraseNode("f_pn_stomach", "stomach", "🤢"),
                        PhraseNode("f_pn_leg", "legs", "🦵"),
                        PhraseNode("f_pn_chest", "chest", "❤️")
                    )
                ),
                PhraseNode("f_cold", "very cold", "🥶", children = listOf(
                    PhraseNode("f_c_blanket", "and need a blanket", "🛏️")
                )),
                PhraseNode("f_hot", "too hot", "🥵", children = listOf(
                    PhraseNode("f_h_fan", "and need the fan", "🌀")
                )),
                PhraseNode("f_tired", "very tired", "😴", children = listOf(
                    PhraseNode("f_t_sleep", "and want to sleep", "🛌")
                )),
                PhraseNode("f_dizzy", "dizzy and lightheaded", "💫"),
                PhraseNode("f_sick", "sick / nauseous", "🤢"),
                PhraseNode("f_thirsty", "thirsty for water", "🥤"),
                PhraseNode("f_uncomf", "uncomfortable in this position", "😣"),
                PhraseNode("f_better", "much better now", "🌤️")
            )
        ),

        // ==========================================
        // BRANCH 6: "Where is..."
        // ==========================================
        PhraseNode(
            id = "b_where_is",
            text = "Where is",
            iconEmoji = "🔍",
            children = listOf(
                PhraseNode("w_is_glasses", "my glasses?", "👓"),
                PhraseNode("w_is_phone", "my phone?", "📱"),
                PhraseNode("w_is_nurse", "the nurse?", "👩‍⚕️"),
                PhraseNode("w_is_doc", "the doctor?", "👨‍⚕️"),
                PhraseNode("w_is_call", "the call button?", "🚨"),
                PhraseNode("w_is_toilet", "the toilet?", "🚻")
            )
        )
    )
}
