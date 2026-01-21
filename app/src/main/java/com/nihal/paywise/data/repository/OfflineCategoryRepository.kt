package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.CategoryDao
import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.CategoryKind
import com.nihal.paywise.domain.model.toDomain
import com.nihal.paywise.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineCategoryRepository(private val categoryDao: CategoryDao) : CategoryRepository {
    override fun getCategoriesByKindStream(kind: CategoryKind): Flow<List<Category>> = 
        categoryDao.observeByKind(kind).map { list -> list.map { it.toDomain() } }

    override fun getAllCategoriesStream(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getCategoryCountByKind(kind: CategoryKind): Int = categoryDao.getCountByKind(kind)

    override suspend fun insertCategory(category: Category) = 
        categoryDao.insert(category.toEntity())
}