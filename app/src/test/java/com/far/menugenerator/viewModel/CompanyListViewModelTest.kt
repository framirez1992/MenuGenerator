package com.far.menugenerator.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.far.menugenerator.model.LoadingState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.storage.CompanyStorage
import com.far.menugenerator.model.storage.MenuStorage
import com.far.menugenerator.utils.CompanyGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.times
import java.lang.RuntimeException

private const val USER = "USER"
private const val FAILURE_MESSAGE = "FAILURE_MESSAGE"

@RunWith(MockitoJUnitRunner::class)
class CompanyListViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule() //Para poder probar liveData
    //Cannot invoke "android.os.Looper.getThread()" because the return value of "android.os.Looper.getMainLooper()" is null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testCoroutineScope = TestScope()

    lateinit var SUT: CompanyListViewModel

    @Mock
    lateinit var companyService: CompanyService
    @Mock
    lateinit var companyStorage: CompanyStorage
    @Mock
    lateinit var menuService: MenuService
    @Mock
    lateinit var menuStorage: MenuStorage

    @Mock
    lateinit var companyListObserver:Observer<List<CompanyFirebase?>?>//OJO los tipos de objetos de los observers deben ser nullables
    @Mock
    lateinit var searchCompanyStateObserver:Observer<LoadingState?>

    @Captor
    lateinit var companyListCaptor:ArgumentCaptor<List<CompanyFirebase?>?>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        SUT = CompanyListViewModel(
            companyService = companyService,
            companyStorage = companyStorage,
            menuService = menuService,
            menuStorage = menuStorage
        )
        //Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun after(){
        SUT.getSearchCompaniesState().removeObserver(searchCompanyStateObserver)
        SUT.getCompanies().removeObserver(companyListObserver)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResume_success_companyObserverNotified() /*runTest */{

        testCoroutineScope.runTest{

            val ac = ArgumentCaptor.forClass(LoadingState::class.java)
            success()
            SUT.getSearchCompaniesState().observeForever(searchCompanyStateObserver)
            SUT.getCompanies().observeForever(companyListObserver)//para  no depender del lifeCicle
            SUT.onResume(USER)
            advanceUntilIdle()//espera que todas las corutinas terminen


            Mockito.verify(searchCompanyStateObserver, times(2)).onChanged(ac.capture())
            val states = ac.allValues
            Assert.assertEquals(states[0].state, State.LOADING)
            Assert.assertEquals(states[1].state, State.SUCCESS)

            Mockito.verify(companyListObserver).onChanged(companyListCaptor.capture())
            Assert.assertEquals(companyListCaptor.value,CompanyGenerator.getCompanies())
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResume_failed_companyObserverNoInteractionsErrorShown()/* = runTest*/ {

        testCoroutineScope.runTest{
            val ac = ArgumentCaptor.forClass(LoadingState::class.java)
            failure()
            SUT.getSearchCompaniesState().observeForever(searchCompanyStateObserver)
            SUT.getCompanies().observeForever(companyListObserver)
            SUT.onResume(USER)
            advanceUntilIdle()//espera que todas las corutinas terminen

            Mockito.verify(searchCompanyStateObserver, times(2)).onChanged(ac.capture())
            val states = ac.allValues
            Assert.assertEquals(states[0].state,State.LOADING)
            Assert.assertEquals(states[1].state, State.ERROR)
            Assert.assertEquals(states[1].message, FAILURE_MESSAGE)
            Mockito.verifyNoInteractions(companyListObserver)
        }

    }


    private suspend fun success() {
        Mockito.`when`(companyService.getCompanies(anyString())).thenReturn(CompanyGenerator.getCompanies())
    }

    private suspend fun failure(){
        Mockito.doThrow(RuntimeException(FAILURE_MESSAGE)).`when`(companyService).getCompanies(anyString())
    }


}


