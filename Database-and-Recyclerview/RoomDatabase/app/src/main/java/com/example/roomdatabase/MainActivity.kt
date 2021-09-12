package com.example.roomdatabase

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.*
import com.example.roomdatabase.databinding.ActivityMainBinding
import com.example.roomdatabase.databinding.ListItemBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import kotlinx.coroutines.flow.collect


private lateinit var binding: ActivityMainBinding
private lateinit var personViewModel: PersonModelView
private lateinit var adapter: MyRecyclerviewAdapter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val dao = PersonDatabase.getInstance(application).personDatabaseDao
        val repository = PersonRepository(dao)
        val factory = PersonViewModelFactory(repository)
        Thread {
            personViewModel = ViewModelProvider(this, factory).get(PersonModelView::class.java)
            binding.myViewModel = personViewModel

        }.start()
        binding.lifecycleOwner = this

        initRecyclerView()
    }

    private fun initRecyclerView() {
        binding.personRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyRecyclerviewAdapter()
        binding.personRecyclerView.adapter = adapter
        displayPersonsList()
    }

    private fun displayPersonsList(){
        personViewModel.getSavedPersons().observe(this, Observer {
            adapter.setList(it)
            adapter.notifyDataSetChanged()
        })
    }
}

//database

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true)
    var personId: Long = 0L,

    @ColumnInfo(name = "person_name")
    val personName: String = "",

    @ColumnInfo(name = "person_age")
    val personAge: String = "",
)

@Dao
interface PersonDatabaseDao {
    @Insert
    suspend fun insertPerson(person: Person): Long

    @Query("SELECT * FROM persons")
    fun getAllPersons(): Flow<List<Person>>

}

@Database(entities = [Person::class], version = 1, exportSchema = false)
abstract class PersonDatabase : RoomDatabase() {
    abstract val personDatabaseDao: PersonDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: PersonDatabase? = null

        fun getInstance(context: Context): PersonDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        PersonDatabase::class.java,
                        "persons")
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

//View Models
class PersonRepository(private val dao: PersonDatabaseDao) {
    val persons = dao.getAllPersons()

    suspend fun insert(person: Person): Long {
        return dao.insertPerson(person)
    }
}

class PersonModelView(private val repository: PersonRepository) : ViewModel() {
    val inputName = MutableLiveData<String>()
    val inputAge = MutableLiveData<String>()

    //display toast when data insert
    private val statusMessage = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>>
        get() = statusMessage

    fun save() {
        val name = inputName.value!!
        val age = inputAge.value!!
        insertPerson(Person(0, name, age))
        inputName.value = ""
        inputAge.value = ""
    }

    private fun insertPerson(person: Person) = viewModelScope.launch {
        repository.insert(person)
    }
    fun getSavedPersons() = liveData {
        repository.persons.collect {
            emit(it)
        }
    }
}

class PersonViewModelFactory(
    private val repository: PersonRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonModelView::class.java)) {
            return PersonModelView(repository) as T
        }
        throw IllegalArgumentException("Unknown View Model class")
    }
}

open class Event<out T>(private val content: T) {
    private var hasBeenHandled = false
        private set // Allow external read but not write

    //Returns the content and prevents its use again
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
        //return the content, even if it#s already handled
        fun peekContent(): T = content
    }
}

class MyRecyclerviewAdapter() :
    RecyclerView.Adapter<MyViewHolder>() {
    private val personList = ArrayList<Person>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: ListItemBinding =
            DataBindingUtil.inflate(layoutInflater, R.layout.list_item, parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return personList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(personList[position])
    }

    fun setList(persons: List<Person>) {
        personList.clear()
        personList.addAll(persons)
    }
}

class MyViewHolder(private val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(person: Person) {
        binding.nameTextView.text = person.personName
        binding.ageTextView.text = person.personAge
    }
}