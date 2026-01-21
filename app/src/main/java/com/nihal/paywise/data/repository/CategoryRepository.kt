package com.nihal.paywise.data.repository

import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.CategoryKind
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesByKindStream(kind: CategoryKind): Flow<List<Category>>
    fun getAllCategoriesStream(): Flow<List<Category>>
    suspend fun getCategoryCountByKind(kind: CategoryKind): Int
    suspend fun insertCategory(category: Category)
}