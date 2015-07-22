#include "../include/Tree.h"
#include <map>
#include <algorithm>

#define MIN2(A,B)       ((A)<(B)?(A):(B))
#define MIN3(A,B,C)     (MIN2(MIN2((A),(B)),(C)))


using std::vector;


bool is_valid_dot_bracket(std::string dot_bracket)
{
    // tests Vienna dot-bracket for illegal structure (or symbol)
    int counter = 0;

    for (unsigned int i = 0; i < dot_bracket.size(); ++i)
    {
        char c = dot_bracket[i];
        if (c == '(')               // stack
        {
            counter += 1;
        }
        else if (c == ')')          // unstack
        {
            counter -= 1;
        }
        else if (c != '.')          // illegal character
        {
            return false;
        }
        if (counter < 0)            // left unbalanced
        {
            return false;
        }
    }
    if (counter != 0)               // right unbalanced
    {
        return false;
    }
    else                            // correct dotbracket
    {
        return true;
    }
}


std::string only_paired(std::string dot_bracket)
{
    // removes the "." characters from the dot_bracket
    assert(is_valid_dot_bracket(dot_bracket));  // only apply on legal dot_bracket
    std::string ret = std::string(dot_bracket);
    ret.erase(std::remove(ret.begin(), ret.end(), '.'), ret.end());
    return ret;
}


std::string db2shape( std::string dbwithdots ){
    char db[dbwithdots.size()];
    int dblen=0;
    for( int i=0; i<dbwithdots.size(); ++i ){
        if( dbwithdots[i]!='.' )
        {
            char c = dbwithdots[i];
            c = c==')'?']':(c=='('?'[':c);
            db[dblen++]=c;
        }
    }

    //compute the idb (integer dot bracket) of the db
    int buddies[dblen];
    int stack[dblen];
    int sp=-1;
    int maxsp = -1; //needed to know if a db is only filled with '.'
    for( int i=0;i<dblen;++i ){
        if(db[i]=='['){
            stack[++sp]=i;
        } else if( db[i]==']' ){
            buddies[i]=stack[sp];
            buddies[stack[sp]]=i;
            --sp;
        }
        maxsp = maxsp>sp?maxsp:sp;
    }

    int slen = 1; //1 and not 0 because if db[0] != '.' then there is no room for the first (
    //1find length of shape
    for( int i=1; i<dblen; ++i ){
        if( ( buddies[i-1]-buddies[i]!=1 ) | ( db[i]!=db[i-1] ) ) ++slen;
    }
    // fill the shape
    std::vector<char> shape = std::vector<char>(slen+1);
    
    int next=0;
    if(maxsp>-1)shape[next++]='[';
    for( int i=1; i<dblen; ++i ){
        if( ( ( buddies[i-1]-buddies[i]!=1 ) | ( db[i]!=db[i-1] ) ) )
            shape[next++]=db[i];
    }
    std::string result = std::string(shape.begin(), shape.begin() + next); //shape[next]=0;
    return result;
}


Node* dot_bracket_to_node(std::string dot_bracket)
{
    //creates a base pair tree from Vienna dot bracket
    assert(is_valid_dot_bracket(dot_bracket));
    Node* root = new Node();
    Node* position = root;
    int node_id = 0;
    for (int i = 0; dot_bracket[i] != '\0'; i++)
    {
        char c = dot_bracket[i];
        if (c == '(')
        {
            Node* child = new Node(position, node_id);
            node_id = node_id + 1;
            position = child;
        }
        else if (c == ')')
        {
            position = position->get_parent();
        }
        else
        {
            continue;
        }
    }
    return root;
}

Tree::Tree(std::string brackets)
{
    // all trees can be represented by a bracket (Vienna dot bracket minus the dots)
    // convert the bracket its corresponding tree structure
    if (! is_valid_dot_bracket(brackets))
    {
        std::cerr << "Badly formed dot bracket fed to tree constructor"
        << std::endl << brackets << std::endl;
        std::exit(EXIT_FAILURE);
    }
    Node* root = dot_bracket_to_node(brackets);

    // string representation
    this->brackets = only_paired(brackets);

    // postorder enumeration of the nodes and label them according to their order
    vector<Node*> nodes = get_postorder_enumeration(root);
    for (unsigned int post_order_index = 0; post_order_index < nodes.size(); ++post_order_index)
        nodes[post_order_index]->set_label(post_order_index);

    // calculate the leftmost descendents
    leftmost_descendents = vector<int>(nodes.size(), 0);
    for (unsigned int node_index=0; node_index <  nodes.size(); ++node_index)
    {
        leftmost_descendents[node_index] = nodes[node_index]->get_leftmost_descendent_label();
    }

    // calculate the keyroots based on the leftmost descendents
    this->keyroots = vector<int>();
    std::map<int, int> keyroots_map;
    int leftmost_descendent_label;
    for(int node_index = nodes.size()-1; node_index > -1; --node_index)
    {
        leftmost_descendent_label= leftmost_descendents[node_index];

        if (!(keyroots_map.count(leftmost_descendent_label)))
        {
            keyroots_map[leftmost_descendent_label] = node_index;
        }
    }
    for (std::map<int, int>::iterator it = keyroots_map.begin(); it != keyroots_map.end(); ++it)
    {
        this->keyroots.push_back((*it).second);
    }

    std::sort(this->keyroots.begin(), this->keyroots.end());

    // free the nodes
    for (std::vector<Node*>::iterator it = nodes.begin() ; it != nodes.end(); ++it)
    {
        delete (*it);
    }
    nodes.clear();

    return;
}


Tree::Tree(const Tree &other)
{
    // copy constructor
    brackets = std::string(other.brackets);
    leftmost_descendents = other.leftmost_descendents;
    keyroots = other.keyroots;
}


Tree::~Tree() { }


std::string Tree::get_brackets() const
{
    return this->brackets;
}


// getters

vector<int> Tree::get_leftmost_descendents() const
{
    return this->leftmost_descendents;
}

vector<int> Tree::get_keyroots() const
{
    return this->keyroots;
}



// comparison operators

bool operator==(const Tree &tree1, const Tree &tree2)
{
    std::string a = tree1.get_brackets();
    std::string b = tree2.get_brackets();
    return (a == b);
}


bool operator!=(const Tree &tree1, const Tree &tree2)
{
    return !(tree1 == tree2);
}

bool operator<(const Tree &tree1, const Tree &tree2)
{
    return tree1.get_brackets() < tree2.get_brackets();

}



// stream operators

std::ostream& operator<< (std::ostream &out, Tree &tree)
{
    out << tree.brackets << std::endl;
    return out;
}


std::ostream& operator<< (std::ostream &out, Tree* tree)
{
    out << *tree;
    return out;
}


void calculate_tree_distance(Tree A, Tree B, int i, int j, vector< vector<int> >& treedists)
{
    vector<int> Al = A.get_leftmost_descendents();
    vector<int> Bl = B.get_leftmost_descendents();
    int p,q;

    int m = i - Al[i] + 2;
    int n = j - Bl[j] + 2;

    // create a m x n matrix of zeros
    vector< vector<int> > forest_distance = vector< vector<int> >(m);
    for(int x = 0; x != m; ++x)
        forest_distance[x].resize(n, 0);

    int ioff = Al[i] - 1;
    int joff = Bl[j] - 1;

    // fill deletions (first row)
    for (int x = 1; x != m; ++x)
        forest_distance[x][0] = forest_distance[x-1][0] + 1;

    // fill insertions (first column)
    for (int y = 1; y != n; ++y)
        forest_distance[0][y] = forest_distance[0][y-1] + 1;
    // fill the matrix
    for (int x = 1; x != m; ++x)
    {
        for (int y = 1; y != n; ++y)
        {
            // case 1
            // x is an ancestor of i and y is an ancestor of j
            if ( (Al[i] == Al[x+ioff]) && (Bl[j] == Bl[y+joff]) )
            {
                forest_distance[x][y] = MIN3( (forest_distance[x-1][y] + 1),  // deletion
                                              (forest_distance[x][y-1] + 1),  // insertion
                                              (forest_distance[x-1][y-1]) );  // substitution

                treedists[x+ioff][y+joff] = forest_distance[x][y];
            }
            // case 2
            else
            {
                p = Al[x+ioff]-1-ioff;
                q = Bl[y+joff]-1-joff;
                forest_distance[x][y] = MIN3((forest_distance[x-1][y] + 1),  // deletion
                                             (forest_distance[x][y-1] + 1),  // insertion
                                             (forest_distance[p][q] + treedists[x+ioff][y+joff]));  // substitution
            }
        }
    }
    return;
}


int unit_distance(const Tree A, const Tree B)
{
    // unlabeled tree distance
    // create and fill the vector that will hold the tree distances computed
    size_t sizeA = (A.get_brackets().size() / 2) + 1; // + 1 for artificial root
    size_t sizeB = (B.get_brackets().size() / 2) + 1;

    // create the matrix holding tree distances, A x B
    vector< vector<int> > tree_distances = vector< vector<int> >(sizeA);
    for(unsigned int x = 0; x !=sizeA; ++x)
    {
        tree_distances[x].resize(sizeB, 0);
    }

    vector<int>::iterator index1;
    vector<int>::iterator index2;
    vector<int> keyrootsA = A.get_keyroots();
    vector<int> keyrootsB = B.get_keyroots();
    for (index1 = keyrootsA.begin(); index1 != keyrootsA.end(); ++index1)
    {
        for(index2 = keyrootsB.begin(); index2 != keyrootsB.end(); ++index2)
        {
            calculate_tree_distance(A, B, *index1, *index2, tree_distances);
        }
    }

    return tree_distances[tree_distances.size()-1][tree_distances[0].size()-1];
}


