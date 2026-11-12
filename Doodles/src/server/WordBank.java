package server;

import java.util.*;

/**
 * Every word the game can ask someone to draw, grouped by category.
 * Words are simple, drawable things so rounds stay fun. Duplicates are removed on load.
 */
public final class WordBank {

    private WordBank() {}

    private static final Map<String, String[]> BANK = new LinkedHashMap<>();

    private static void add(String category, String csv) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String w : csv.split(",")) {
            String t = w.trim().toUpperCase(Locale.ROOT);
            if (!t.isEmpty()) set.add(t);
        }
        BANK.put(category, set.toArray(new String[0]));
    }

    static {
        add("Animals", "tiger,penguin,elephant,lion,monkey,dolphin,cat,dog,giraffe,snake,zebra,rabbit,turtle,octopus,butterfly,spider,shark,whale,owl,eagle,frog,bear,panda,kangaroo,horse,cow,pig,sheep,chicken,duck,crab,bee,ant,snail,camel,fox,wolf,deer,hedgehog,flamingo,parrot,peacock,squirrel,bat,crocodile,jellyfish,seahorse,koala,hippo,rhino");
        add("Food", "pizza,burger,cake,sushi,donut,popcorn,sandwich,noodles,ice cream,chocolate,apple,banana,strawberry,watermelon,pineapple,cookie,pancake,egg,cheese,bread,hot dog,french fries,taco,cupcake,lollipop,pasta,soup,salad,carrot,corn,tomato,grapes,lemon,cherry,peach,coconut,mushroom,pretzel,waffle,popsicle,avocado,broccoli,pumpkin,honey,candy,sausage,cereal,milkshake,burrito,dumpling");
        add("Technology", "laptop,phone,robot,camera,keyboard,drone,headphones,mouse,printer,television,satellite,microphone,battery,charger,speaker,joystick,smartwatch,tablet,router,usb drive,monitor,flashlight,radio,calculator,wifi,rocket,telescope,circuit,webcam,game console,vr headset,remote control,light bulb,power plug,hard drive,microchip,antenna,projector,scanner,e-book");
        add("Fantasy", "dragon,wizard,unicorn,castle,sword,ghost,fairy,vampire,mermaid,magic wand,witch,werewolf,zombie,troll,giant,elf,genie,phoenix,knight,crown jewel,treasure chest,crystal ball,potion,spell book,magic carpet,pegasus,centaur,goblin,ogre,monster,haunted house,shield,flying broom,fire breath,skeleton,mummy,alien,time machine,magic mirror,dungeon");
        add("Objects", "hammer,chair,clock,umbrella,guitar,crown,backpack,bottle,lamp,scissors,key,pencil,book,candle,mirror,bucket,broom,ladder,table,sofa,bed,pillow,towel,toothbrush,comb,glasses,watch,ring,necklace,wallet,suitcase,balloon,kite,envelope,stapler,tape,fork,spoon,knife,plate,cup,teapot,vase,bell,flag,lock,compass,magnet,anchor,globe");
        add("Places", "beach,castle,school,hospital,airport,museum,library,park,restaurant,stadium,farm,zoo,bakery,bridge,lighthouse,volcano,island,desert,jungle,mountain,waterfall,cave,igloo,windmill,pyramid,circus,playground,fire station,gas station,supermarket,cinema,hotel,tower,harbor,farmhouse,tent,treehouse,swimming pool,museum,city skyline");
        add("Actions", "running,swimming,dancing,sleeping,jumping,cooking,reading,singing,flying,laughing,crying,climbing,painting,fishing,skiing,surfing,sneezing,yawning,whispering,waving,clapping,kicking,throwing,catching,diving,juggling,skating,digging,hugging,pointing,knocking,brushing teeth,riding a bike,taking a selfie,playing guitar,blowing bubbles,flying a kite,building a snowman,walking the dog,drinking");
        add("Nature", "tree,flower,sun,moon,cloud,rainbow,lightning,snowflake,tornado,mountain,river,ocean,wave,leaf,cactus,rose,sunflower,mushroom,volcano,star,planet,comet,forest,campfire,rain,snowman,icicle,seashell,pebble,grass,palm tree,pine tree,sunrise,sunset,galaxy,iceberg,coral,puddle,feather,acorn");
        add("Sports", "football,basketball,tennis,golf,cricket,baseball,boxing,archery,bowling,hockey,volleyball,badminton,rugby,skateboard,surfboard,trophy,medal,whistle,goal,racket,helmet,bicycle,swimming goggles,ping pong,karate,yoga,weightlifting,marathon,gymnastics,fencing,snowboard,diving board,finish line,referee,cheerleader,penalty kick,bat and ball,pool table,dart board,chess");
        add("Jobs", "doctor,teacher,pilot,chef,firefighter,police officer,farmer,astronaut,painter,singer,dentist,nurse,builder,artist,fisherman,judge,magician,clown,pirate,scientist,mechanic,barber,waiter,photographer,detective,king,queen,soldier,librarian,gardener,musician,plumber,electrician,postman,lifeguard,baker,cowboy,ninja,superhero,programmer");
        add("Vehicles", "car,bus,train,airplane,helicopter,bicycle,motorcycle,boat,ship,submarine,rocket,tractor,ambulance,fire truck,taxi,scooter,hot air balloon,sailboat,canoe,tram,truck,police car,ice cream truck,skateboard,jet ski,cable car,spaceship,parachute,wheelchair,snowmobile,school bus,garbage truck,cruise ship,racing car,unicycle,rickshaw,steam engine,bulldozer,crane,ufo");
        add("Clothes", "hat,shoes,socks,jacket,dress,t-shirt,jeans,scarf,gloves,sunglasses,boots,sandals,sweater,tie,belt,cap,raincoat,pajamas,swimsuit,skirt,shorts,apron,crown,mask,backpack,bow tie,hoodie,slippers,top hat,cowboy hat,lab coat,uniform,helmet,mittens,umbrella,flip flops,suit,costume,wig,overalls");
        add("Space", "rocket,astronaut,moon,sun,earth,mars,saturn,comet,meteor,galaxy,black hole,telescope,satellite,space station,alien,ufo,constellation,eclipse,planet,star,rover,orbit,asteroid,solar system,milky way,launch pad,space suit,shooting star,crater,jupiter,nebula,gravity,lunar module,space shuttle,big dipper,sputnik");
        add("Music", "guitar,piano,drum,violin,trumpet,flute,saxophone,microphone,headphones,harp,accordion,banjo,cello,tambourine,xylophone,music note,orchestra,concert,dj,vinyl record,jukebox,choir,conductor,bagpipes,maracas,ukulele,bass guitar,recorder,triangle,cymbals,speaker,karaoke,radio,rock star,singer,band,metronome,sheet music,boombox,gramophone");
        add("School", "pencil,eraser,ruler,notebook,backpack,blackboard,teacher,homework,exam,library,bell,desk,globe,calculator,scissors,glue,crayon,microscope,science lab,school bus,uniform,graduation,diploma,lunch box,chalk,paintbrush,map,compass,alphabet,textbook,locker,playground,recess,sharpener,paper plane,whiteboard,projector,marker,trophy,spelling bee");
        add("Holidays", "christmas tree,santa claus,snowman,pumpkin,fireworks,gift,easter egg,candy cane,reindeer,stocking,wreath,jack o lantern,turkey,heart,cupid,birthday cake,balloon,party hat,confetti,pinata,lantern,diya,haunted house,witch,bunny,sleigh,mistletoe,gingerbread man,champagne,new year clock,parade,costume,trick or treat,sparkler,advent calendar,snow globe,angel,elf,bonfire,rangoli");
        add("Everyday", "toothpaste,soap,shampoo,toilet,shower,bathtub,sink,fridge,oven,microwave,washing machine,iron,vacuum cleaner,trash can,doormat,curtain,window,door,staircase,elevator,mailbox,fence,garden hose,lawn mower,wheelbarrow,toolbox,paint can,screwdriver,saw,nail,drill,measuring tape,flashlight,fire extinguisher,smoke alarm,ceiling fan,air conditioner,heater,blanket,alarm clock");
    }

    public static List<String> categories() { return new ArrayList<>(BANK.keySet()); }

    public static String[] words(String category) { return BANK.getOrDefault(category, new String[0]); }

    public static int totalWords() {
        int n = 0;
        for (String[] w : BANK.values()) n += w.length;
        return n;
    }

    /** One word choice: category + word. */
    public static final class Choice {
        public final String category, word;
        Choice(String category, String word) { this.category = category; this.word = word; }
    }

    /**
     * Picks {@code count} words for the drawer to choose from: every word comes from a
     * different category and none of them was used recently in the room.
     */
    public static List<Choice> pick(int count, Random rnd, Set<String> recent) {
        List<String> cats = categories();
        Collections.shuffle(cats, rnd);
        List<Choice> out = new ArrayList<>();
        Set<String> taken = new HashSet<>();
        for (String cat : cats) {
            if (out.size() >= count) break;
            String[] words = words(cat);
            if (words.length == 0) continue;
            String w = null;
            for (int tries = 0; tries < 20 && w == null; tries++) {
                String cand = words[rnd.nextInt(words.length)];
                if (!recent.contains(cand) && !taken.contains(cand)) w = cand;
            }
            if (w == null) continue;
            taken.add(w);
            out.add(new Choice(cat, w));
        }
        return out;
    }

    /** 1 = easy, 2 = medium, 3 = hard (by length and number of words). */
    public static int difficulty(String word) {
        int letters = word.replace(" ", "").length();
        boolean multi = word.contains(" ");
        if (letters <= 4 && !multi) return 1;
        if (letters >= 9 || (multi && letters >= 8)) return 3;
        return 2;
    }
}
