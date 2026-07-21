package lobby.db;

import java.nio.file.Files;
import java.nio.file.Path;
import model.tablemodel.TableModel;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CustomRoomRepositoryTest {
	@Test
	public void savesAndRestoresCustomModel() throws Exception {
		Path file = Files.createTempFile("lobby-", ".db");
		SqliteDatabase database = new SqliteDatabase(file.toString());
		database.initSchema();
		CustomRoomRepository repository = new CustomRoomRepository(database);
		TableModel model = new TableModel(); model.setId(10000); model.setType(2); model.setSeatNum(3); model.setCardNum(17);
		assertEquals(true, repository.save(model, "tester"));
		assertEquals(1, repository.listEnabled().size());
		assertEquals(10000, repository.listEnabled().get(0).getId());
		Files.deleteIfExists(file);
	}
}
