package explorer.world.block.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.ColorPack;
import explorer.world.block.PlanetBoundBlock;

/**
 * Created by RYZEN on 21.12.2017.
 */

/**
 * No to tak ten blok to przykład ładnie opisany i jest to tak naprawde DirtBlock
 * Jak widzisz dla testow nazwa klasy to PapekBlock słowo extends oznacza że ta klasa dziedziczy po klasie co nazywa się PlanetBoundBlock natomiast ta klasa dziedziczy po klasie Block
 * Wystarczy ze wiesz ze tak naprawde dziedziczy po klasie Block oraz posiada wbudowany w siebie kolor block określony zmienna block_color
 */

public class PapekBlock extends PlanetBoundBlock {

    /**
     * Tutaj jest ten twój pakiet możliwych kolorów które może przybrać ten blok
     * Jest to tylko tak zwany reference (jak się bawiłes w c++ to coś jak pointer) aby być w stanie mieć dostęp do tej zmiennej z innej klasy
     */

    private ColorPack color_pack;

    /**
     *  Tutaj jest konstruktor jest to taka jakby funkcja wywołyana gdy tworzymy blok (nie stawiamy block ale gdy gra tworzy instancje bloku w czasie ładowania świata, więc kod tutaj wykona się tylko raz)
     *  Jak widać ma 2 parametry:
     *  -instancja świata gry
     *  -instancja klasy głownej gry(taka klasa która wszystko łączy dzięki czemu gra działa)
     */
    public PapekBlock(World world, Game game) {
        super(world, game);

        /**
         * Tutaj określamy podstawowe parametry bloku
         * block_id czyli ID bloku musi byc unikalne dla każdego bloku
         *
         * block_group określa w jaki sposób ten blok łączy się wizualnie z innymi np gdy graniczy z innym wybierana jest teksutura gdzie wszystko się ładnie łączy
         * aktualnie sa 3 mozliwe opcje CONNECT_WITH_EVERYTHING czyli laczy sie z kazdym blokiem, CONNECT_WITH_NOTHING czyli z niczym sie dany blok nie łaczy (czyli wykorzysywana jest tylko 1 tekstura)
         * oraz CONNECT_WITH_SAME_BLOCK czyli łączy się tylko z takimi samymi jak ten blok
         */

        this.block_id = 2;
        this.block_group = BlockGroup.CONNECT_WITH_SAME_BLOCK;

        //parametr określający czy blok jest przeszkoda dla fizyki czy tylko ozdobą
        this.collidable = true;

        //parametr dzięki którmu blok tła znajdujący się na pozycji tej samej co blok przedni oraz gdy blok przedni będzie tym tutaj blokiem to blok tyli będzie się renderował
        //chodzi o to że masz np blok pochodnia i nie zajmuje on wszystkich pikseli i są puste piksels (których alpha = 0) żeby nie było prześwitów na tło trzeba wyrenderwoać blok tła co własnie robi ta flaga gdy = true)
        this.need_background_block_rendered = false;

        //tutaj trochę juz trudniej wytłumaczyć chodzi o to że blok tła będzie się renderował gdy sposób tego połączenia nie będzie tekstura że łączy się ze wszystkimi stronami pozwala naprawić problem gdzie są pute piksele i widać tło
        //trzeba to ustawić na true gdy któryś z 16 tekstur danego bloku posiada gdzieś piksel z alpha = 0 aby uniknąć prześwitania tła
        this.need_background_block_rendered_if_not_fully_surrounded = false;

        //Tutaj mamy hash mapę (czyli jest to taka tablica posiadajaca zestawy 2 rzeczy klucza i wartość, działa tak że za pomocą klucza jesteśmy w stanie uzyskać wartość)
        this.tile_positions = new HashMap<Short, TextureRegion>();

        //Tutaj zaczynamy ładować kolor packa, najpierw tworzymy instancje naszej klasy typu ColorPack która przechowuje wyszystkie możliwe kolory dla danego bloku
        color_pack = new ColorPack();

        //Tutaj bierzemy jaka tekstura jest tym kolor pakiem, tak długi kod ponieważ kazdy typ planety bedzie miał inny typ takiej tekstury więc to rozwiąznie jest
        //uniwersalne i modyfikujemy tylko wartośći po stronie PlanetType i możemy mieć typów planet na ile nam RAM pozwoli
        String colorpack_region_name = world.getPlanetProperties().PLANET_TYPE.BLOCKS_PROPERTIES.getBlockProperties(DirtBlock.class).COLOR_PACK_REGION_NAME; // czyli wartość uzyskana tutaj to będzie "blocks/dirt_colorpack" wartość ta ustawiona jest w typie planety zobacz klase TestBlockProperties (world/planet/planet_type/types/TestBlocksProperties)

        //Tutaj za pomoca wyzej znalezionej scieżki do tekstury od kolor paka w końcu ładujemy naszego kolor paka z dysku
        //Ta zmienna PLANET_FACTOR określa który kolor wybrać jest to unikalana losowa licza dla każdej planety (losowa ale stała więc spoko zawsze będzie taka sama dla danej planety)
        block_color.set(color_pack.load(world.getPlanetProperties().PLANET_FACTOR, game.getAssetsManager().getTextureRegion(colorpack_region_name)));

        //Poniżej ładujemy wszystkie tekstury (czyli 16) dla bloku dirta

        //ta zmienna określa rozmiar tekstury w pikselach czyli zmieniasz na taka jaka masz zrobione
        final int BLOCK_PIXEL_SIZE = 16;

        //Ta linijka kodu ładuje teksture która ma w sobie te 16 podtekstur (wszystkie możliwe połączenie bloków ta tekstura ktore mi wysyłałeś) i dzieli je na osobne dzięki czemu można je rysować na ekranie, dlatego BLOCK_PIXEL_SIZE musi być dobrą liczba inaczje tekstury będą skopane
        TextureRegion[][] textures = game.getAssetsManager().getTextureRegion("blocks/dirt_spritesheet").split(BLOCK_PIXEL_SIZE, BLOCK_PIXEL_SIZE);

        //Te wszystkie linijki kodu to tak naprawde określanie dla każdej możliwej pozycji bloku odpowiedniej tekstury
        //np COLLIDE_NONE to tekstura uzywana gdy mamy blok dirtu nie otoczony żadnym innym (same powietrze wookoło) dlatego uzywamy tekstury z samego górnego lewego rogu (odpal swoją teksture od dirta gdzie są wszystkie pozycje i zobaczysz że się zgadza)
        tile_positions.put(Block.COLLIDE_NONE, textures[0][0]);
        tile_positions.put(Block.COLLIDE_ALL_SIDES, textures[1][2]);

        tile_positions.put(Block.COLLIDE_LEFT, textures[3][1]);
        tile_positions.put(Block.COLLIDE_LEFT_DOWN, textures[0][3]);
        tile_positions.put(Block.COLLIDE_LEFT_DOWN_RIGHT, textures[0][2]);
        tile_positions.put(Block.COLLIDE_LEFT_RIGHT, textures[3][2]);
        tile_positions.put(Block.COLLIDE_LEFT_UP, textures[2][3]);
        tile_positions.put(Block.COLLIDE_LEFT_UP_DOWN, textures[1][3]);
        tile_positions.put(Block.COLLIDE_LEFT_UP_RIGHT, textures[2][2]);
        tile_positions.put(Block.COLLIDE_DOWN, textures[1][0]);
        tile_positions.put(Block.COLLIDE_RIGHT, textures[3][0]);
        tile_positions.put(Block.COLLIDE_RIGHT_DOWN, textures[0][1]);
        tile_positions.put(Block.COLLIDE_UP, textures[2][0]);
        tile_positions.put(Block.COLLIDE_UP_DOWN, textures[3][3]);
        tile_positions.put(Block.COLLIDE_UP_RIGHT, textures[2][1]);
        tile_positions.put(Block.COLLIDE_UP_RIGHT_DOWN, textures[1][1]);

        //I teraz najważniejsze aby blok był w stanie być uzywany przez silnik gry
        //Spójrz na klasę Blocks (explorer/world/block i tutaj jest)
        //Jak widzisz instancja kazdego bloku musi się tam znajdować inaczej nie będzei działa zapobiega to tworzeniu wielu instancji jedngeo bloku co było by baaaardzo nie wyjdaje
        //bo tworzenie takie bloku o tutaj o dziwo nie jest takie szybkie jak moze sie wydawac (color pack np dlugo bd sie ładował bo czyta kolory pikseli z tekstury co miałe dla wydajności nie jest)
        //Ten blok tutaj oczywiscie nie jest dodany do tamtej listy bo to tylko ładnie opisany blok DirtBlock zobacz jak własnie DirtBlock jest tam dodany
    }
}

