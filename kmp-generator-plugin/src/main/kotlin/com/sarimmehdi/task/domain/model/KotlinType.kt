package com.sarimmehdi.task.domain.model

import java.io.Serial
import java.io.Serializable

sealed class KotlinType(
    open val isNullable: Boolean,
) : Serializable {
    data class KotlinString(
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = 1094105001576175596L
        }
    }

    data class KotlinInt(
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = -7693991557400178010L
        }
    }

    data class KotlinLong(
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = -7745696708452122445L
        }
    }

    data class KotlinDouble(
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = -838240907922868468L
        }
    }

    data class KotlinFloat(
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = 6968464132981355381L
        }
    }

    data class KotlinBoolean(
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = 6749791708251846834L
        }
    }

    data class KotlinList(
        val itemType: KotlinType,
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = 5673791630064901308L
        }
    }

    data class KotlinSet(
        val itemType: KotlinType,
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: kotlin.Long = -3488277414165531653L
        }
    }

    data class KotlinMap(
        val keyType: KotlinType,
        val valueType: KotlinType,
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: kotlin.Long = -3302382697461439204L
        }
    }

    data class Custom(
        val name: String,
        val packageName: String? = null,
        override val isNullable: Boolean = false,
    ) : KotlinType(isNullable) {
        companion object {
            @Serial
            private const val serialVersionUID: kotlin.Long = -2497328944780372281L
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: kotlin.Long = 779342273774767266L
    }
}
