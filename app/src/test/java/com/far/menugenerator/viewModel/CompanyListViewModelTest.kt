package com.far.menugenerator.viewModel
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.storage.CompanyStorage
import com.far.menugenerator.model.storage.MenuStorage
import com.far.menugenerator.utils.CompanyGenerator
import com.far.menugenerator.utils.MenuGenerator
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.RuntimeException

private const val USER = "USER"
private const val FAILURE_MESSAGE = "FAILURE_MESSAGE"
private const val REMOTE_FILE_NAME = "REMOTE_FILE_NAME"

private const val NOT_FOUND = 404

class CompanyListViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()

    private lateinit var sut: CompanyListViewModel

    @RelaxedMockK
    lateinit var companyService: CompanyService
    @RelaxedMockK
    lateinit var companyStorage: CompanyStorage
    @RelaxedMockK
    lateinit var menuService: MenuService
    @RelaxedMockK
    lateinit var menuStorage: MenuStorage


    @Before
    fun abc(){
        Dispatchers.setMain(dispatcher)
        MockKAnnotations.init(this)
        sut = CompanyListViewModel(
            companyService = companyService,
            companyStorage = companyStorage,
            menuService = menuService,
            menuStorage = menuStorage
        )
    }


    @Test
    fun onResume_success_companyObserverNotified() =runTest{

        /*var job = launch(dispatcher) {

        }*/
        val capturedStates = mutableListOf<ProcessState?>()
        //para atrapar varios states. porque el rule InstantTaskExecutorRule te dara solo el ultimo estado cunado llames el observable.value
        sut.getSearchCompaniesState().observeForever(){
            capturedStates.add(it)
        }

        success()
        sut.onResume(USER)
        dispatcher.scheduler.advanceUntilIdle()//espera que todas las corutinas terminen

        val companies = sut.getCompanies().value //esperar a que se llene el value con el advanceUntilIdle

        Assert.assertEquals(capturedStates.first()?.state, State.LOADING)
        Assert.assertEquals(capturedStates.last()?.state, State.SUCCESS)

        Assert.assertEquals(companies,CompanyGenerator.getCompanies())



    }


    @Test
    fun onResume_failed_companyObserverNoInteractionsErrorShown() = runTest {

        val capturedStates = mutableListOf<ProcessState?>()

        failure()
        sut.getSearchCompaniesState().observeForever{
            capturedStates.add(it)
        }

        sut.onResume(USER)
        dispatcher.scheduler.advanceUntilIdle()
        val companies = sut.getCompanies().value

        Assert.assertEquals(capturedStates.first()?.state,State.LOADING)
        Assert.assertEquals(capturedStates.last()?.state, State.ERROR)
        Assert.assertEquals(capturedStates.last()?.message, FAILURE_MESSAGE)
        Assert.assertNull(companies)
    }

    @Test
    fun getCompanies_success_companyObserversNotified() = runTest {
        val capturedStates = mutableListOf<ProcessState?>()
        success()
        sut.getSearchCompaniesState().observeForever{
            capturedStates.add(it)
        }
        sut.getCompanies(user = USER)
        dispatcher.scheduler.advanceUntilIdle()
        val companies = sut.getCompanies().value

        Assert.assertEquals(capturedStates.first()?.state,State.LOADING)
        Assert.assertEquals(capturedStates.last()?.state, State.SUCCESS)
        Assert.assertEquals(companies, CompanyGenerator.getCompanies())

    }

    @Test
    fun getCompanies_failed_errorNotified()= runTest {
        val capturedStates = mutableListOf<ProcessState?>()
        failure()
        sut.getSearchCompaniesState().observeForever{
            capturedStates.add(it)
        }
        sut.getCompanies(user = USER)
        dispatcher.scheduler.advanceUntilIdle()
        val companies = sut.getCompanies().value
        Assert.assertEquals(capturedStates.first()?.state,State.LOADING)
        Assert.assertEquals(capturedStates.last()?.state, State.ERROR)
        Assert.assertEquals(capturedStates.last()?.message, FAILURE_MESSAGE)
        Assert.assertNull(companies)
    }

    @Test
    fun deleteCompany_success_allDependenciesDeleted() = runTest {
        val deleteStates = mutableListOf<ProcessState?>()
        success()
        sut.getDeleteCompanyState().observeForever{
            deleteStates.add(it)
        }
        val companyToDelete = CompanyGenerator.getCompanies().first()!!
        sut.deleteCompany(USER,companyToDelete)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify {
            if(!companyToDelete.logoUrl.isNullOrEmpty())
            companyStorage.removeCompanyLogo(USER,companyToDelete.companyId, companyToDelete.logoFileName!!)
        }
        MenuGenerator.generateMenus().forEach{
            coVerify {
                menuStorage.removeAllMenuFiles(user = USER, menuId = it?.menuId!!)
                menuService.deleteMenu(USER,companyToDelete.companyId,it)
            }

        }

        verify { companyService.deleteCompany(USER,companyToDelete)}

        Assert.assertEquals(deleteStates.first()?.state, State.LOADING)
        Assert.assertEquals(deleteStates.last()?.state,State.SUCCESS)

    }

    @Test
    fun deleteCompany_failed_onCompanyLogoDelete() = runTest{
        val deleteStates = mutableListOf<ProcessState?>()
        failureCompanyLogoDelete()
        sut.getDeleteCompanyState().observeForever {
            deleteStates.add(it)
        }
        val company = CompanyGenerator.getCompanies().first()
        sut.deleteCompany(USER,company!!)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { companyStorage.removeCompanyLogo(user = USER, companyId = company.companyId,company.logoFileName!!) }
        Assert.assertEquals(deleteStates.first()?.state,State.LOADING)
        Assert.assertEquals(deleteStates.last()?.state,State.ERROR)

    }

    @Test
    fun `deleteCompany success when fileNotFound exception was thrown`() = runTest{
        val states = mutableListOf<ProcessState?>()
        successWithFileNoFoundException()
        sut.getDeleteCompanyState().observeForever {
            states.add(it)
        }
        val company = CompanyGenerator.getCompanies().first()
        sut.deleteCompany(user = USER, company = company!!)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { companyStorage.removeCompanyLogo(user = USER, companyId = company.companyId,company.logoFileName!!) }
        coVerify { menuStorage.removeAllMenuFiles(user = USER, menuId = any()) }
        coVerify { menuService.deleteMenu(user = USER, companyId = company.companyId,any()) }
        coVerify { companyService.deleteCompany(user = USER, company = company) }

        Assert.assertEquals(states.first()?.state,State.LOADING)
        Assert.assertEquals(states.last()?.state,State.SUCCESS)
    }



    private suspend fun success() {
        coEvery { companyService.getCompanies(any()) } returns CompanyGenerator.getCompanies()
        coEvery { menuService.getMenus(user = any(), companyId = any())} returns MenuGenerator.generateMenus()
    }

    private suspend fun successWithFileNoFoundException(){
        coEvery {companyService.getCompanies(any())} returns CompanyGenerator.getCompanies()
        coEvery { menuService.getMenus(any(),any()) } returns MenuGenerator.generateMenus()

        val ex:StorageException? = StorageException.fromExceptionAndHttpCode(
            Exception(FAILURE_MESSAGE),
            NOT_FOUND)

        val fbEx = FirebaseFirestoreException(FAILURE_MESSAGE,FirebaseFirestoreException.Code.NOT_FOUND)

        coEvery { companyStorage.removeCompanyLogo(any(),any(),any()) } throws ex!!
        coEvery { menuStorage.removeAllMenuFiles(any(),any()) } throws ex
        coEvery { menuService.deleteMenu(any(),any(),any()) } throws fbEx
        coEvery { companyService.deleteCompany(any(),any()) } throws fbEx

    }


    private suspend fun failure(){
        coEvery { companyService.getCompanies(any()) } throws RuntimeException(FAILURE_MESSAGE)
    }

    private suspend fun failureCompanyLogoDelete(){
        coEvery { companyService.getCompanies(any()) } returns CompanyGenerator.getCompanies()
        coEvery { companyStorage.removeCompanyLogo(any(),any(),any()) } throws RuntimeException(FAILURE_MESSAGE)
    }

}


